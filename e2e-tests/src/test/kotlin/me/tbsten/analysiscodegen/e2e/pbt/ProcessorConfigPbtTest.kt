package me.tbsten.analysiscodegen.e2e.pbt

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.shuffle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import me.tbsten.analysiscodegen.pbt.run.acgCheckAll
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PbtSpec
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import me.tbsten.analysiscodegen.testing.CodegenTestResult
import me.tbsten.analysiscodegen.testing.TestEngine
import me.tbsten.analysiscodegen.testing.runCodegenTest
import me.tbsten.analysiscodegen.testprocessors.TpAnnotationsSource
import me.tbsten.analysiscodegen.testprocessors.ksp.ChainKspProvider
import me.tbsten.analysiscodegen.testprocessors.ksp.DeferKspProvider
import me.tbsten.analysiscodegen.testprocessors.ksp.MapperKspProvider
import me.tbsten.analysiscodegen.testprocessors.ksp.RegistryKspProvider
import me.tbsten.analysiscodegen.testprocessors.ksp.ResourceKspProvider
import kotlin.time.Duration.Companion.minutes

// PBT-05: ランダムな Processor 構成（連鎖の段数・defer・出力の形）で、必ず停止し、生成物が実行順に依存しない
// Processor はテスト用 Processor（TP-*、ADR-027）の KSP 版を TestEngine.AnalysisCodegen（Bridge）で動かす。
// 「停止する」= 時間内に結果が返り、成功するか、最大ラウンド超過の診断で失敗する（ADR-014）

/** 入力と Processor の構成。[order] は Processor の登録順。 */
internal data class ProcessorConfig(
    /** TP-Chain の起点ごとの段数（0 なら連鎖しない）。 */
    val chainDepths: List<Int>,
    /** TP-Defer: 連鎖の n 段目ができるまで待つクラス。-1 は存在しないクラスを待つ（最後まで解決しない）。 */
    val deferTargets: List<Int>,
    /** TP-Resource: Java とリソースを出すクラスの数。 */
    val javaOutputs: Int,
    /** TP-Mapper の対象クラスの数。 */
    val mapped: Int,
    val order: List<String>,
) {
    fun sources(): Map<String, String> =
        buildMap {
            put("me/tbsten/analysiscodegen/tp/${TpAnnotationsSource.FILE_NAME}", TpAnnotationsSource.content)
            chainDepths.forEachIndexed {
                i,
                d,
                ->
                put("cfg/C$i.kt", "package cfg\n\n@me.tbsten.analysiscodegen.tp.Chain(depth = $d) class C$i")
            }
            deferTargets.forEachIndexed { i, t ->
                val target = if (t < 0 || chainDepths.isEmpty()) "cfg.Missing$i" else "cfg.C${t % chainDepths.size}Chain${1 + t % 3}"
                put("cfg/D$i.kt", "package cfg\n\n@me.tbsten.analysiscodegen.tp.DeferUntil(\"$target\") class D$i")
            }
            repeat(javaOutputs) {
                put(
                    "cfg/J$it.kt",
                    "package cfg\n\n@me.tbsten.analysiscodegen.tp.GenerateJava @me.tbsten.analysiscodegen.tp.Registered class J$it",
                )
            }
            repeat(
                mapped,
            ) { put("cfg/M$it.kt", "package cfg\n\n@me.tbsten.analysiscodegen.tp.Mapped class M$it(val v: Int, val s: String)") }
        }

    companion object {
        val PROVIDERS: Map<String, () -> SymbolProcessorProvider> =
            linkedMapOf(
                "Chain" to ::ChainKspProvider,
                "Defer" to ::DeferKspProvider,
                "Resource" to ::ResourceKspProvider,
                "Registry" to ::RegistryKspProvider,
                "Mapper" to ::MapperKspProvider,
            )
    }
}

internal fun Arb.Companion.processorConfig(): Arb<ProcessorConfig> =
    arbitrary { rs ->
        ProcessorConfig(
            chainDepths = Arb.list(Arb.int(0..6), 0..4).bind(),
            deferTargets = Arb.list(Arb.int(-1..9), 0..3).bind(),
            javaOutputs = Arb.int(0..3).bind(),
            mapped = Arb.int(0..3).bind(),
            order = Arb.shuffle(ProcessorConfig.PROVIDERS.keys.toList()).bind(),
        )
    }

private suspend fun runConfig(
    config: ProcessorConfig,
    order: List<String>,
): CodegenTestResult =
    withTimeout(5.minutes) {
        withContext(Dispatchers.IO) {
            runCodegenTest(TestEngine.AnalysisCodegen) {
                config.sources().forEach { (path, content) -> kotlin(path, content) }
                order.forEach { ksp(ProcessorConfig.PROVIDERS.getValue(it)()) }
                options["tp.processors"] = order.joinToString(",")
            }
        }
    }

class ProcessorConfigPbtTest : PbtSpec({
    case(
        "PBT-05",
        "ランダムな Processor 構成（連鎖の段数・defer・出力の形）で、必ず停止し、生成物が実行順に依存しない",
        pending = PendingCase(Milestone.M2),
    ) {
        acgCheckAll("PBT-05 Processor の構成によらず停止し、生成物が実行順に依存しない", Arb.processorConfig()) { config ->
            // withTimeout が切れたら停止しなかったとして失敗する
            val first = runConfig(config, config.order)
            val reversed = runConfig(config, config.order.reversed())
            val unresolvable = config.deferTargets.any { it < 0 } || (config.deferTargets.isNotEmpty() && config.chainDepths.isEmpty())

            withClue(config) {
                if (unresolvable) {
                    // 最後まで解決しない defer は、最終ラウンドの診断（既定は Error、ADR-014）で止まる
                    first.isSuccess shouldBe false
                    first.errors.any { "DeferUntil" in it.message || "defer" in it.message.lowercase() } shouldBe true
                } else {
                    first.isSuccess shouldBe true
                    // 連鎖は段数ぶんだけ作られる
                    config.chainDepths.forEachIndexed { i, d ->
                        first.generatedFiles.keys.count { it.substringAfterLast('/').startsWith("C${i}Chain") } shouldBe d
                    }
                }
                // 登録順を逆にしても、成否・生成物・エラーは同じ
                reversed.isSuccess shouldBe first.isSuccess
                reversed.generatedFiles shouldBe first.generatedFiles
                reversed.errors.map { it.message }.sorted() shouldBe first.errors.map { it.message }.sorted()
            }
        }
    }
})

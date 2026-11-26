@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.enginetests.inc

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import me.tbsten.analysis.code.gen.enginetests.support.EngineProject
import me.tbsten.analysis.code.gen.incremental.DirtyFileCalculator
import me.tbsten.analysis.code.gen.incremental.IncrementalPlan
import me.tbsten.analysis.code.gen.incremental.IncrementalState
import me.tbsten.analysis.code.gen.incremental.InputSnapshot
import me.tbsten.analysis.code.gen.incremental.OptionChangeJudge
import me.tbsten.analysis.code.gen.incremental.OutputDependencyKind
import me.tbsten.analysis.code.gen.incremental.OutputRecord
import me.tbsten.analysis.code.gen.incremental.PersistentCache
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessor
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorEnvironment
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorProvider
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysis.code.gen.processor.Dependencies
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import me.tbsten.analysis.code.gen.processor.OptionImpact
import me.tbsten.analysis.code.gen.testfoundation.IntegrationSpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import java.nio.file.Files

// 5.10 増分処理（INC）のうち、増分エンジンの内部（ADR-015）を直接見る I

private data class State(
    override val snapshot: InputSnapshot,
    override val outputs: List<OutputRecord>,
) : IncrementalState

/** 依存を宣言せずに（Dependencies.all）、全ファイル数を書く Processor。 */
class UndeclaredDepsProvider : AnalysisCodegenProcessorProvider {
    override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor =
        object : AnalysisCodegenProcessor {
            override fun process(context: AnalysisCodegenRoundContext) {
                if (context.round != 1) return
                val total = context.allFiles.sumOf { it.declarations.size }
                context.codeGenerator.generateKotlin("gen", "Count", Dependencies.all, "package gen\n\nconst val DECLS = $total\n")
            }
        }
}

class IncrementalEngineTest : IntegrationSpec({
    case("INC-13", "依存を宣言しなかった出力は、関連する入力全体に依存するものとして扱われる", pending = PendingCase(Milestone.M2)) {
        // 1) 計画: All の出力は、どのソースが変わっても作り直しの対象
        val sources = mapOf("src/main/kotlin/A.kt" to "a1", "src/main/kotlin/B.kt" to "b1")
        val previous =
            State(
                InputSnapshot(sources, "g", emptyMap()),
                listOf(OutputRecord("kotlin/gen/Count.kt", "test", OutputDependencyKind.All, emptySet(), emptySet())),
            )
        val plan =
            DirtyFileCalculator().plan(
                previous,
                InputSnapshot(sources + ("src/main/kotlin/B.kt" to "b2"), "g", emptyMap()),
            ) { _, _ -> emptySet() }
        when (plan) {
            is IncrementalPlan.Full -> {}
            is IncrementalPlan.Incremental -> {
                plan.staleOutputs shouldContain "kotlin/gen/Count.kt"
                plan.dirtySources shouldBe sources.keys
            }
        }

        // 2) エンジン: A.kt に関係しない B.kt の変更でも、Count が作り直される
        val project =
            EngineProject().apply {
                kotlin("A.kt", "class A")
                kotlin("B.kt", "class B")
                nativeProcessor(UndeclaredDepsProvider::class)
            }
        val engine =
            me.tbsten.analysis.code.gen.engine
                .AnalysisCodegenEngine()
        project.run(engine).success shouldBe true
        project.generated().values.single() shouldBe "package gen\n\nconst val DECLS = 2\n"
        project.kotlin("B.kt", "class B\nclass B2")
        project.run(engine).success shouldBe true
        project.generated().values.single() shouldBe "package gen\n\nconst val DECLS = 3\n"
    }

    case("INC-15", "analysisCodeGen / Kotlin のバージョンやキャッシュ形式が変わると、永続キャッシュが無効化される", pending = PendingCase(Milestone.M2)) {
        val dir = Files.createTempDirectory("acg-cache").toFile()
        val cache = PersistentCache(dir)
        val state = State(InputSnapshot(mapOf("A.kt" to "1"), "kotlin=2.4.21;acg=0.1.0", emptyMap()), emptyList())
        cache.save("format-1", state)
        cache.load("format-1").shouldNotBeNull().snapshot shouldBe state.snapshot
        // キャッシュ形式が違えば読まない
        cache.load("format-2").shouldBeNull()
        // Kotlin・analysisCodeGen のバージョンは globalFingerprint に入り、違えば全件処理
        val plan =
            DirtyFileCalculator().plan(
                state,
                state.snapshot.copy(globalFingerprint = "kotlin=2.4.30;acg=0.1.0"),
            ) { _, _ -> emptySet() }
        plan.shouldBeInstanceOf<IncrementalPlan.Full>()
        val plan2 =
            DirtyFileCalculator().plan(
                state,
                state.snapshot.copy(globalFingerprint = "kotlin=2.4.21;acg=0.2.0"),
            ) { _, _ -> emptySet() }
        plan2.shouldBeInstanceOf<IncrementalPlan.Full>()
    }

    // ADR-051: オプションの影響は Processor がキーごとに宣言する。宣言の無いキーは Full
    val provider =
        object : AnalysisCodegenProcessorProvider {
            override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor = error("このテストでは使わない")

            override fun optionImpact(key: String): OptionImpact =
                when (key) {
                    "inc.incremental" -> OptionImpact.Incremental
                    "inc.none" -> OptionImpact.None
                    else -> super.optionImpact(key)
                }
        }

    case(
        "INC-18",
        "オプションが変わったとき、キー単位で `Full` が 1 つでもあれば全件、`Incremental` だけなら増分、`None` だけなら無視になる（宣言の無いキーは `Full`。ADR-051）",
        pending = PendingCase(Milestone.M2),
    ) {
        val before = mapOf("inc.incremental" to "1", "inc.none" to "1", "other" to "1")

        fun judge(after: Map<String, String>) = OptionChangeJudge.judge(before, after, provider::optionImpact)
        // 変化なし
        judge(before) shouldBe OptionImpact.None
        // None だけ / Incremental だけ
        judge(before + ("inc.none" to "2")) shouldBe OptionImpact.None
        judge(before + ("inc.incremental" to "2")) shouldBe OptionImpact.Incremental
        judge(before + ("inc.incremental" to "2") + ("inc.none" to "2")) shouldBe OptionImpact.Incremental
        // 宣言の無いキー（値の変更・追加・削除）は Full。1 つでも Full があれば全体が Full
        provider.optionImpact("other") shouldBe OptionImpact.Full
        judge(before + ("other" to "2")) shouldBe OptionImpact.Full
        judge(before + ("new" to "1")) shouldBe OptionImpact.Full
        judge(before - "other") shouldBe OptionImpact.Full
        judge(before + ("inc.incremental" to "2") + ("other" to "2")) shouldBe OptionImpact.Full
        // KSP Bridge 経由の Processor は宣言を持たないので常に Full
        OptionChangeJudge.judge(before, before + ("inc.none" to "2")) { OptionImpact.Full } shouldBe OptionImpact.Full
    }

    case("INC-19", "Processor が `Incremental` と宣言したオプションを変えても、全ファイルは dirty にならず、増分のまま処理される（ADR-051）", pending = PendingCase(Milestone.M2)) {
        val sources = mapOf("A.kt" to "a1", "B.kt" to "b1")
        val previous = State(InputSnapshot(sources, "g", emptyMap()), emptyList())
        // Incremental 宣言のオプションは globalFingerprint に入らないので、変わっても指紋は同じ
        OptionChangeJudge.judge(mapOf("inc.incremental" to "1"), mapOf("inc.incremental" to "2"), provider::optionImpact) shouldBe
            OptionImpact.Incremental
        val plan = DirtyFileCalculator().plan(previous, InputSnapshot(sources, "g", emptyMap())) { _, _ -> emptySet() }
        val incremental = plan.shouldBeInstanceOf<IncrementalPlan.Incremental>()
        incremental.dirtySources shouldBe emptySet()
    }
})

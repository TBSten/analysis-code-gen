@file:OptIn(InternalAnalysisCodegenApi::class, ExperimentalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.enginetests.pbt

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import me.tbsten.analysis.code.gen.engine.AnalysisCodegenEngine
import me.tbsten.analysis.code.gen.enginetests.support.EngineProject
import me.tbsten.analysis.code.gen.enginetests.support.KspShapeProvider
import me.tbsten.analysis.code.gen.enginetests.support.NativeShapeProvider
import me.tbsten.analysis.code.gen.ksp.hybrid.withAnalysis
import me.tbsten.analysis.code.gen.pbt.change.ChangeHistory
import me.tbsten.analysis.code.gen.pbt.change.changeHistory
import me.tbsten.analysis.code.gen.pbt.run.acgCheckAll
import me.tbsten.analysis.code.gen.pbt.source.render
import me.tbsten.analysis.code.gen.processor.ExperimentalAnalysisCodegenApi
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PbtSpec
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import org.jetbrains.kotlin.psi.KtClassOrObject
import java.io.File
import com.google.devtools.ksp.processing.Dependencies as KspDependencies

// 増分の最重要の不変条件 FullBuild(最終入力) == IncrementalBuild(変更履歴, 最終入力) を、ランダムな変更履歴で確かめる。
// 毎時の定期実行（pbt-hourly.yml、ADR-022）で回る。PR では回帰 seed と固定 seed だけ

/** Hybrid 版（HYB-05）: クラスのメンバー数を withAnalysis（PSI）で数え、読んだファイルを Dependencies に明示する。 */
class HybridShapeProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        object : SymbolProcessor {
            private var done = false

            override fun process(resolver: Resolver): List<KSAnnotated> {
                if (done) return emptyList()
                done = true
                resolver.getNewFiles().filter { it.fileName.endsWith(".kt") }.forEach { file ->
                    file.declarations.filterIsInstance<KSClassDeclaration>().forEach { cls ->
                        val count = cls.withAnalysis { symbol -> (symbol.psi as? KtClassOrObject)?.declarations?.size ?: -1 }
                        val pkg = file.packageName.asString().ifEmpty { "shapes.root" }
                        val name = cls.simpleName.asString()
                        environment.codeGenerator.createNewFile(KspDependencies(false, file), pkg, "${name}Count").use {
                            it.write("package $pkg\n\nobject ${name}Count { val members = $count }\n".toByteArray())
                        }
                    }
                }
                return emptyList()
            }
        }
}

/** [EngineProject] のソースを、PBT の 1 状態（1 モジュール）と同じ中身にそろえる。 */
private fun EngineProject.syncTo(files: Map<String, String>) {
    File(root, "src/main").deleteRecursively()
    files.forEach { (path, content) ->
        // `<モジュール>/src/main/(kotlin|java)/...` → `src/main/(kotlin|java)/...`
        write(path.substringAfter('/'), content)
    }
}

private fun filesOf(state: me.tbsten.analysis.code.gen.pbt.change.BuildState): Map<String, String> = render(state.project).files

/** 履歴を同じエンジン（= 増分）で順に流した最終結果と、最終状態をクリーンに 1 回流した結果。 */
private fun incrementalVsClean(
    history: ChangeHistory,
    setup: EngineProject.() -> Unit,
): Pair<Map<String, String>, Map<String, String>> {
    val states = history.states()
    val incremental = EngineProject().apply(setup)
    val engine = AnalysisCodegenEngine()
    states.forEach { s ->
        incremental.syncTo(filesOf(s))
        check(incremental.run(engine).success) { "増分ビルドが失敗した" }
    }
    val clean = EngineProject().apply(setup)
    clean.syncTo(filesOf(states.last()))
    check(clean.run().success) { "クリーンビルドが失敗した" }
    return incremental.generated() to clean.generated()
}

class IncrementalPropertyTest : PbtSpec({
    case("INC-14", "ランダムな変更履歴（追加・変更・削除・移動・依存変更）のあとの増分結果が、クリーンビルドと一致する。1 時間ごとの定期実行で回す", pending = PendingCase(Milestone.M2)) {
        acgCheckAll("INC-14 増分 = クリーン（Native）", Arb.changeHistory()) { history ->
            val (incremental, clean) = incrementalVsClean(history) { nativeProcessor(NativeShapeProvider::class) }
            incremental shouldBe clean
        }
    }

    case("BINC-04", "Bridge 版でも、ランダムな変更履歴のあとの増分結果がクリーンビルドと一致する", pending = PendingCase(Milestone.M3)) {
        acgCheckAll("BINC-04 増分 = クリーン（Bridge）", Arb.changeHistory()) { history ->
            val (incremental, clean) = incrementalVsClean(history) { kspProcessor(KspShapeProvider::class.java.name) }
            incremental shouldBe clean
        }
    }

    case("MIX-06", "Native と KSP を混ぜた構成でも、増分結果がクリーンビルドと一致する", pending = PendingCase(Milestone.M3)) {
        // 同じ出力名にならないよう、KSP 版は Hybrid の <Name>Count、Native 版は <Name>Shape を作る
        acgCheckAll("MIX-06 増分 = クリーン（Native + KSP）", Arb.changeHistory()) { history ->
            val (incremental, clean) =
                incrementalVsClean(history) {
                    nativeProcessor(NativeShapeProvider::class)
                    kspProcessor(KspCountProvider::class.java.name)
                }
            incremental shouldBe clean
        }
    }

    case("HYB-05", "Hybrid の増分（KSP 部分は自動追跡、AA 部分は明示依存）で、増分結果がクリーンビルドと一致する", pending = PendingCase(Milestone.M3)) {
        acgCheckAll("HYB-05 増分 = クリーン（Hybrid）", Arb.changeHistory()) { history ->
            val (incremental, clean) = incrementalVsClean(history) { kspProcessor(HybridShapeProvider::class.java.name) }
            incremental shouldBe clean
        }
    }
})

/** MIX-06 用の KSP 版: クラスごとに `<Name>Count`（KSP API だけでメンバー数を数える）。 */
class KspCountProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        object : SymbolProcessor {
            private var done = false

            override fun process(resolver: Resolver): List<KSAnnotated> {
                if (done) return emptyList()
                done = true
                resolver.getNewFiles().filter { it.fileName.endsWith(".kt") }.forEach { file ->
                    file.declarations.filterIsInstance<KSClassDeclaration>().forEach { cls ->
                        val pkg = file.packageName.asString().ifEmpty { "shapes.root" }
                        val name = cls.simpleName.asString()
                        environment.codeGenerator.createNewFile(KspDependencies(false, file), pkg, "${name}Count").use {
                            it.write("package $pkg\n\nobject ${name}Count { val members = ${cls.declarations.count()} }\n".toByteArray())
                        }
                    }
                }
                return emptyList()
            }
        }
}

@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysiscodegen.enginetests.fail

import io.kotest.matchers.shouldBe
import me.tbsten.analysiscodegen.engine.AnalysisCodegenEngine
import me.tbsten.analysiscodegen.enginetests.support.EngineProject
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessor
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorEnvironment
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorProvider
import me.tbsten.analysiscodegen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysiscodegen.processor.Dependencies
import me.tbsten.analysiscodegen.processor.InternalAnalysisCodegenApi
import me.tbsten.analysiscodegen.testfoundation.IntegrationSpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import java.io.File

// 5.12 失敗からの回復（FAIL）のうち I。ラウンドの commit と永続の commit は分かれている（ADR-014）

/** ラウンド 1 でクラスごとに生成し、ソースに `FAIL_IN_ROUND_2` があればラウンド 2 で例外を投げる。 */
class FailInRound2Provider : AnalysisCodegenProcessorProvider {
    override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor =
        object : AnalysisCodegenProcessor {
            private var shouldFail = false

            override fun process(context: AnalysisCodegenRoundContext) {
                if (context.round == 1) {
                    shouldFail = context.allFiles.any { "FAIL_IN_ROUND_2" in it.text }
                    context.newFiles.forEach { f ->
                        val name = f.name.removeSuffix(".kt")
                        context.codeGenerator.generateKotlin("gen", "${name}Gen", Dependencies.isolating(f), "package gen\n\nobject ${name}Gen\n")
                    }
                } else if (context.round == 2 && shouldFail) {
                    error("FAIL-04: intentional failure in round 2")
                }
            }
        }
}

private fun File.snapshot(): Map<String, String> =
    if (!isDirectory) emptyMap() else walkTopDown().filter { it.isFile }.associate { it.relativeTo(this).invariantSeparatorsPath to it.readBytes().contentHashCode().toString() }

class RoundFailureTest : IntegrationSpec({
    case("FAIL-04", "ラウンド 2 で失敗したとき、永続キャッシュは前回成功時のまま（ラウンドの commit と永続の commit が分かれている）", pending = PendingCase(Milestone.M2)) {
        val project =
            EngineProject().apply {
                kotlin("A.kt", "class A")
                nativeProcessor(FailInRound2Provider::class)
            }
        val engine = AnalysisCodegenEngine()
        project.run(engine).success shouldBe true
        val cacheDir = File(project.root, "build/analysisCodeGen/caches/main")
        val cacheBefore = cacheDir.snapshot()
        val outputBefore = project.generated()
        (cacheBefore.isNotEmpty()) shouldBe true

        // ラウンド 1 は成功する（B の生成物を Round Commit する）が、ラウンド 2 で失敗する
        project.kotlin("B.kt", "class B { val marker = \"FAIL_IN_ROUND_2\" }")
        project.run(engine).success shouldBe false
        cacheDir.snapshot() shouldBe cacheBefore
        project.generated() shouldBe outputBefore

        // 直したら、前回成功時からの増分で正しく作り直せる
        project.kotlin("B.kt", "class B")
        project.run(engine).success shouldBe true
        project.generated().keys.map { it.substringAfterLast('/') }.toSet() shouldBe setOf("AGen.kt", "BGen.kt")
    }
})

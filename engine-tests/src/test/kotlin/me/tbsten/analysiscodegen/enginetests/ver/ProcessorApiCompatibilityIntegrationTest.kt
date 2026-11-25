package me.tbsten.analysiscodegen.enginetests.ver

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysiscodegen.enginetests.support.NATIVE_SERVICE
import me.tbsten.analysiscodegen.testfoundation.IntegrationSpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import me.tbsten.analysiscodegen.testing.TestEngine
import me.tbsten.analysiscodegen.testing.runCodegenTest

// 5.23 バージョン互換（VER）のうち analysis-code-gen-processor-api の版違い（VER-02）。診断の決まりは ADR-013「バージョン非互換の診断」:
// Processor の呼び出しで LinkageError が出たら、Processor 名・framework のバージョン・「analysis-code-gen-processor-api の非互換の可能性」を出して失敗させる

/**
 * 「新しすぎる analysis-code-gen-processor-api」の写し。本物にない `buildId` を環境に足してある。
 * これでコンパイルした Processor は、本物の analysis-code-gen-processor-api の上では `buildId` を呼んだところで LinkageError になる。
 */
private val FUTURE_PROCESSOR_API =
    mapOf(
        "me/tbsten/analysiscodegen/processor/FutureApi.kt" to
            """
            package me.tbsten.analysiscodegen.processor

            interface AnalysisCodegenRoundContext

            interface AnalysisCodegenProcessor {
                fun process(context: AnalysisCodegenRoundContext)

                fun finish()

                fun onError()
            }

            interface AnalysisCodegenProcessorEnvironment {
                val options: Map<String, String>

                // 本物の analysis-code-gen-processor-api に無いメンバー（将来の版で足された想定）
                val buildId: String
            }

            fun interface AnalysisCodegenProcessorProvider {
                fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor
            }
            """,
    )

private const val FUTURE_PROVIDER = "acg.ver02.FutureApiProvider"

private val FUTURE_PROCESSOR =
    mapOf(
        "acg/ver02/FutureApiProvider.kt" to
            """
            package acg.ver02

            import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessor
            import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorEnvironment
            import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorProvider
            import me.tbsten.analysiscodegen.processor.AnalysisCodegenRoundContext

            class FutureApiProvider : AnalysisCodegenProcessorProvider {
                override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor {
                    val id = environment.buildId
                    return object : AnalysisCodegenProcessor {
                        override fun process(context: AnalysisCodegenRoundContext) {
                            println(id)
                        }

                        override fun finish() {}

                        override fun onError() {}
                    }
                }
            }
            """,
    )

class ProcessorApiCompatibilityIntegrationTest : IntegrationSpec({
    case(
        "VER-02",
        "非互換な analysis-code-gen-processor-api バージョンでビルドされた Native Processor では、原因が分かる診断が出る",
        pending = PendingCase(Milestone.M2),
    ) {
        // 1. 将来の analysis-code-gen-processor-api の写しをコンパイルし、2. それだけを見て Processor をコンパイルする（本物の analysis-code-gen-processor-api は見ない）
        val futureApi = compileToClasses(FUTURE_PROCESSOR_API, classpath = listOf(kotlinStdlib), inherit = false)
        val processorClasses = compileToClasses(FUTURE_PROCESSOR, classpath = listOf(futureApi, kotlinStdlib), inherit = false)
        // JAR には Processor だけを入れる（analysis-code-gen-processor-api は実行時にエンジンが渡す本物を使う）
        val jar = processorJar(processorClasses, mapOf(NATIVE_SERVICE to listOf(FUTURE_PROVIDER)))

        val result =
            runCodegenTest(TestEngine.AnalysisCodegen) {
                kotlin("com/example/A.kt", "package com.example\n\nclass A")
                processorJar(jar)
            }

        withClue(result.messages) {
            result.isSuccess shouldBe false
            val error = result.errors.single().message
            // どの Processor か・何が原因か・どの版の framework で動かしたか
            error shouldContain FUTURE_PROVIDER
            error shouldContain "analysis-code-gen-processor-api の非互換の可能性"
            error shouldContain Regex("""\d+\.\d+""")
        }
    }
})

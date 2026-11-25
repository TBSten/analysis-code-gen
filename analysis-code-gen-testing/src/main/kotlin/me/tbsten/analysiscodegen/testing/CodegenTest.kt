package me.tbsten.analysiscodegen.testing

import me.tbsten.analysiscodegen.testing.internal.AnalysisCodegenTestRunner
import me.tbsten.analysiscodegen.testing.internal.Ksp2TestRunner

/**
 * ソース文字列と Processor を渡して、生成・コンパイルまで回す（TST-01〜03）。
 *
 * ```
 * val result = runCodegenTest(TestEngine.Ksp2) {
 *     kotlin("com/example/Foo.kt", "@Hello class Foo")
 *     ksp(HelloProcessorProvider())
 * }
 * result.isSuccess shouldBe true
 * result.generatedFile("FooHello.kt") shouldContain "fun hello()"
 * result.loadClass("com.example.FooHello")
 * ```
 */
public fun runCodegenTest(
    engine: TestEngine = TestEngine.Ksp2,
    block: CodegenTestSpec.() -> Unit,
): CodegenTestResult {
    val spec = CodegenTestSpec().apply(block)
    return runSpec(engine, spec)
}

internal fun runSpec(
    engine: TestEngine,
    spec: CodegenTestSpec,
): CodegenTestResult =
    when (engine) {
        TestEngine.Ksp2 -> Ksp2TestRunner.run(spec)
        TestEngine.AnalysisCodegen -> AnalysisCodegenTestRunner.run(spec)
    }

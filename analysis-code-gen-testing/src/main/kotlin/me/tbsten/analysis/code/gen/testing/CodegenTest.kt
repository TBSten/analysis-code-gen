package me.tbsten.analysis.code.gen.testing

import me.tbsten.analysis.code.gen.testing.internal.AnalysisCodegenTestRunner
import me.tbsten.analysis.code.gen.testing.internal.IsolatedClasspath

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
        TestEngine.Ksp2 -> runKsp2Isolated(spec)
        TestEngine.AnalysisCodegen -> AnalysisCodegenTestRunner.run(spec)
    }

/**
 * KSP2（kct）は AA（*-for-ide）と同じ ClassLoader では壊れるため、AA を除いた ClassLoader で動かす（ADR-065）。
 * spec・結果・KSP の公開 API はこちらと共有するので、Provider のインスタンスをそのまま渡せる。
 */
private fun runKsp2Isolated(spec: CodegenTestSpec): CodegenTestResult {
    val runner = Class.forName("me.tbsten.analysis.code.gen.testing.internal.Ksp2TestRunner", true, IsolatedClasspath.sharingTestApi)
    val instance = runner.getField("INSTANCE").get(null)
    val run = runner.getMethod("run", CodegenTestSpec::class.java)
    val thread = Thread.currentThread()
    val previous = thread.contextClassLoader
    thread.contextClassLoader = IsolatedClasspath.sharingTestApi
    return try {
        run.invoke(instance, spec) as CodegenTestResult
    } catch (e: java.lang.reflect.InvocationTargetException) {
        throw e.targetException
    } finally {
        thread.contextClassLoader = previous
    }
}

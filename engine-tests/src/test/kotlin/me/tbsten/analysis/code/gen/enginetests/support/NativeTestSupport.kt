package me.tbsten.analysis.code.gen.enginetests.support

import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessor
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorEnvironment
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorProvider
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysis.code.gen.processor.Dependencies
import me.tbsten.analysis.code.gen.testing.CodegenTestResult
import me.tbsten.analysis.code.gen.testing.CodegenTestSpec
import me.tbsten.analysis.code.gen.testing.TestEngine
import me.tbsten.analysis.code.gen.testing.runCodegenTest
import org.jetbrains.kotlin.psi.KtFile

/**
 * Native Processor を analysisCodeGen のエンジンで回す（ADR-021・038）。エンジンが入るまでは NotImplementedError で落ちる（保留のまま）。
 *
 * ```
 * val seen = mutableListOf<Int>()
 * val result = runNative {
 *     kotlin("a/A.kt", "package a\nclass A")
 *     native(processorProvider { ctx -> seen += ctx.round })
 * }
 * ```
 */
fun runNative(block: CodegenTestSpec.() -> Unit): CodegenTestResult = runCodegenTest(TestEngine.AnalysisCodegen, block)

/** process だけを持つ Processor の Provider。[onCreate] は create のたびに呼ばれる。 */
fun processorProvider(
    onCreate: (AnalysisCodegenProcessorEnvironment) -> Unit = {},
    onFinish: () -> Unit = {},
    onError: () -> Unit = {},
    process: (AnalysisCodegenRoundContext) -> Unit,
): AnalysisCodegenProcessorProvider =
    AnalysisCodegenProcessorProvider { env ->
        onCreate(env)
        object : AnalysisCodegenProcessor {
            override fun process(context: AnalysisCodegenRoundContext) = process(context)

            override fun finish() = onFinish()

            override fun onError() = onError()
        }
    }

/** ラウンド 1 でだけ Kotlin ファイルを 1 つ生成する Provider。 */
fun generateOnceProvider(
    packageName: String,
    fileName: String,
    content: String,
): AnalysisCodegenProcessorProvider =
    processorProvider { ctx ->
        if (ctx.round == 1) ctx.codeGenerator.generateKotlin(packageName, fileName, Dependencies.all, content)
    }

/** ファイル名だけ（`A.kt`）。比較を読みやすくするため。 */
val List<KtFile>.names: List<String> get() = map { it.name }

/** Native Processor の ServiceLoader のサービス名。 */
const val NATIVE_SERVICE: String = "me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorProvider"

/** KSP Processor の ServiceLoader のサービス名。 */
const val KSP_SERVICE: String = "com.google.devtools.ksp.processing.SymbolProcessorProvider"

/**
 * `META-INF/services/<サービス名>` だけを入れた JAR を作る（Processor の component の代わり。ADR-017）。
 * クラス本体はテストの classpath にあるものを使う。
 */
fun serviceJar(services: Map<String, List<String>>): java.io.File {
    val jar =
        kotlin.io.path
            .createTempFile("acg-processor", ".jar")
            .toFile()
    java.util.jar.JarOutputStream(jar.outputStream()).use { out ->
        services.forEach { (service, impls) ->
            out.putNextEntry(java.util.jar.JarEntry("META-INF/services/$service"))
            out.write(impls.joinToString("\n", postfix = "\n").toByteArray())
            out.closeEntry()
        }
    }
    return jar
}

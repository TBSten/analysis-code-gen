package me.tbsten.analysiscodegen.testing.internal

import me.tbsten.analysiscodegen.testing.CodegenDiagnostic
import me.tbsten.analysiscodegen.testing.CodegenTestResult
import me.tbsten.analysiscodegen.testing.CodegenTestSpec
import me.tbsten.analysiscodegen.testing.TestEngine
import me.tbsten.analysiscodegen.testing.TestProcessor
import me.tbsten.analysiscodegen.testing.kct.DiagnosticSeverity
import me.tbsten.analysiscodegen.testing.kct.KotlinCompilation
import me.tbsten.analysiscodegen.testing.kct.SourceFile
import me.tbsten.analysiscodegen.testing.kct.configureKsp
import me.tbsten.analysiscodegen.testing.kct.kspSourcesDir
import me.tbsten.analysiscodegen.testing.kct.useKsp2
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

/** [TestEngine.Ksp2]: kct fork の KSP2 経路で 1 回動かす。 */
internal object Ksp2TestRunner {
    fun run(spec: CodegenTestSpec): CodegenTestResult {
        require(spec.isSingleSourceSet()) {
            "TestEngine.Ksp2 は SourceSet 1 つ（main）だけに対応する。KMP の SourceSet は TestEngine.AnalysisCodegen か Gradle フィクスチャで試す"
        }
        val workingDir = spec.workingDir ?: Files.createTempDirectory("acg-ksp2-test").toFile()
        val output = ByteArrayOutputStream()
        val compilation =
            KotlinCompilation().apply {
                this.workingDir = workingDir
                sources = spec.sourceSets.values.flatMap { ss -> ss.files.map { SourceFile.new(it.path, it.content) } }
                inheritClassPath = spec.inheritClassPath
                classpaths = spec.classpath.toList()
                languageVersion = spec.languageVersion
                messageOutputStream = output
                useKsp2()
                configureKsp {
                    symbolProcessorProviders +=
                        spec.processors.flatMap {
                            when (it) {
                                is TestProcessor.Ksp -> listOf(it.provider)
                                is TestProcessor.Jar -> kspProvidersIn(it.file)
                                is TestProcessor.Native ->
                                    throw IllegalArgumentException(
                                        "Native Processor は TestEngine.Ksp2 では動かせない（TestEngine.AnalysisCodegen を使う）",
                                    )
                            }
                        }
                    processorOptions.putAll(spec.options)
                }
            }
        val result = compilation.compile()
        val generatedRoot = compilation.kspSourcesDir
        return CodegenTestResult(
            engine = TestEngine.Ksp2,
            isSuccess = result.exitCode == KotlinCompilation.ExitCode.OK,
            generatedFiles = readTree(generatedRoot),
            diagnostics = result.diagnosticMessages.map { CodegenDiagnostic(it.severity.toCodegen(), it.message) },
            messages = result.messages,
            outputDirectory = result.outputDirectory,
            classpath = compilation.classpaths,
        )
    }

    private fun readTree(root: File): Map<String, String> =
        if (!root.isDirectory) {
            emptyMap()
        } else {
            root
                .walkTopDown()
                .filter { it.isFile }
                .associate { it.relativeTo(root).invariantSeparatorsPath to it.readText() }
                .toSortedMap()
        }

    private fun DiagnosticSeverity.toCodegen() =
        when (this) {
            DiagnosticSeverity.ERROR -> CodegenDiagnostic.Severity.Error
            DiagnosticSeverity.WARNING -> CodegenDiagnostic.Severity.Warning
            DiagnosticSeverity.INFO -> CodegenDiagnostic.Severity.Info
            DiagnosticSeverity.LOGGING -> CodegenDiagnostic.Severity.Logging
        }

    /** JAR の META-INF/services にある KSP の Provider。Native の Provider があれば KSP 側は使わない（ADR-017 と同じ規則）。 */
    private fun kspProvidersIn(jar: File): List<com.google.devtools.ksp.processing.SymbolProcessorProvider> {
        val loader = java.net.URLClassLoader(arrayOf(jar.toURI().toURL()), Ksp2TestRunner::class.java.classLoader)
        val zip = java.util.zip.ZipFile(jar)
        val hasNative =
            zip.use {
                it.getEntry("META-INF/services/me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorProvider") !=
                    null
            }
        if (hasNative) return emptyList()
        return java.util.ServiceLoader
            .load(com.google.devtools.ksp.processing.SymbolProcessorProvider::class.java, loader)
            .toList()
    }
}

@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.testing.internal

import me.tbsten.analysis.code.gen.engine.AnalysisCodegenEngine
import me.tbsten.analysis.code.gen.engine.ClasspathProcessorComponent
import me.tbsten.analysis.code.gen.engine.Diagnostic
import me.tbsten.analysis.code.gen.engine.EngineRequest
import me.tbsten.analysis.code.gen.engine.EngineResult
import me.tbsten.analysis.code.gen.engine.InstanceProcessorComponent
import me.tbsten.analysis.code.gen.engine.spi.ProcessorComponent
import me.tbsten.analysis.code.gen.model.AnalysisConfig
import me.tbsten.analysis.code.gen.model.CompilationModel
import me.tbsten.analysis.code.gen.model.CompilerOptionsModel
import me.tbsten.analysis.code.gen.model.DeferredSeverityModel
import me.tbsten.analysis.code.gen.model.ExecutionUnitModel
import me.tbsten.analysis.code.gen.model.KspBridgeConfig
import me.tbsten.analysis.code.gen.model.KspProcessingModeModel
import me.tbsten.analysis.code.gen.model.OutputDirsModel
import me.tbsten.analysis.code.gen.model.PlatformKind
import me.tbsten.analysis.code.gen.model.ProcessingScopeModel
import me.tbsten.analysis.code.gen.model.ProjectModel
import me.tbsten.analysis.code.gen.model.SourceSetModel
import me.tbsten.analysis.code.gen.model.UnitKind
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import me.tbsten.analysis.code.gen.processor.ProcessingScope
import me.tbsten.analysis.code.gen.testing.CodegenDiagnostic
import me.tbsten.analysis.code.gen.testing.CodegenTestResult
import me.tbsten.analysis.code.gen.testing.CodegenTestSpec
import me.tbsten.analysis.code.gen.testing.TestEngine
import me.tbsten.analysis.code.gen.testing.TestProcessor
import me.tbsten.analysis.code.gen.testing.TestSourceSet
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

/**
 * [TestEngine.AnalysisCodegen]: spec を ProjectModel にして AnalysisCodegenEngine で回し、生成物を kct fork でコンパイルする（ADR-021・061）。
 *
 * - SourceSet ごとに実行単位を 1 つ作り、`dependsOn` の上流から順に流す
 * - Processor はインスタンスのまま渡す（`InstanceProcessorComponent`）。JAR は classpath の component にする
 * - [CodegenTestResult.generatedFiles] のキーは出力ルートからの相対パス（`kotlin/…`）。SourceSet が 2 つ以上なら先頭に `<SourceSet>/` が付く
 * - KMP の SourceSet も、コンパイルはまとめて JVM として行う（expect / actual を含むものは M2 以降）
 */
internal object AnalysisCodegenTestRunner {
    fun run(spec: CodegenTestSpec): CodegenTestResult {
        val workingDir = spec.workingDir ?: Files.createTempDirectory("acg-engine-test").toFile()
        val root = File(workingDir, "project").apply { mkdirs() }
        val sourceSets =
            spec.sourceSets.values.filter {
                it.files.isNotEmpty() ||
                    it.name != CodegenTestSpec.MAIN ||
                    spec.sourceSets.size == 1
            }
        for (ss in sourceSets) {
            for (file in ss.files) {
                val dir = if (file.path.endsWith(".java")) "java" else "kotlin"
                File(root, "src/${ss.name}/$dir/${file.path}").apply {
                    parentFile.mkdirs()
                    writeText(file.content)
                }
            }
        }
        val testClasspath = if (spec.inheritClassPath) hostClasspath() else emptyList()
        val classpath = (spec.classpath + testClasspath).distinct()
        val model = model(spec, root, sourceSets, classpath)
        val components = spec.processors.mapIndexed { index, p -> component(index, p) }
        val engine = AnalysisCodegenEngine()

        val results = linkedMapOf<String, EngineResult>()
        for (ss in topologicalOrder(sourceSets)) {
            results[ss.name] = engine.run(EngineRequest(model, ss.name, forceFull = true, extraComponents = components))
            if (!results.getValue(ss.name).success) break
        }

        val multi = sourceSets.size > 1
        val generated = sortedMapOf<String, String>()
        for (ss in sourceSets) {
            val out = File(root, outputBase(ss.name))
            readTree(out).forEach { (path, content) -> generated[if (multi) "${ss.name}/$path" else path] = content }
        }
        val diagnostics =
            results.flatMap { (unit, r) ->
                r.diagnostics.map { d -> (if (multi) "[$unit]" else "") to d }
            }
        val rendered = diagnostics.joinToString("\n") { (prefix, d) -> prefix + d.render() }
        val generationOk = results.size == sourceSets.size && results.values.all { it.success }
        val codegenDiagnostics = diagnostics.map { (_, d) -> CodegenDiagnostic(d.severity.toCodegen(), d.message) }
        if (!generationOk) {
            return CodegenTestResult(TestEngine.AnalysisCodegen, false, generated, codegenDiagnostics, rendered, null, classpath)
        }
        return compile(spec, workingDir, root, sourceSets, generated, codegenDiagnostics, rendered, classpath)
    }

    private fun compile(
        spec: CodegenTestSpec,
        workingDir: File,
        root: File,
        sourceSets: List<TestSourceSet>,
        generated: Map<String, String>,
        diagnostics: List<CodegenDiagnostic>,
        rendered: String,
        classpath: List<File>,
    ): CodegenTestResult {
        val sources =
            sourceSets.flatMap { ss -> ss.files.map { File(root, "src/${ss.name}/${dirOf(it.path)}/${it.path}") } } +
                sourceSets.flatMap { ss ->
                    readTree(File(root, outputBase(ss.name)))
                        .keys
                        .filter { it.endsWith(".kt") || it.endsWith(".java") }
                        .map { File(root, "${outputBase(ss.name)}/$it") }
                }
        val compiled = IsolatedKct.compile(File(workingDir, "compile"), sources, classpath, spec.languageVersion)
        val compileDiagnostics =
            compiled.diagnostics.map { (severity, message) ->
                CodegenDiagnostic(
                    when (severity) {
                        "ERROR" -> CodegenDiagnostic.Severity.Error
                        "WARNING" -> CodegenDiagnostic.Severity.Warning
                        "INFO" -> CodegenDiagnostic.Severity.Info
                        else -> CodegenDiagnostic.Severity.Logging
                    },
                    message,
                )
            }
        return CodegenTestResult(
            engine = TestEngine.AnalysisCodegen,
            isSuccess = compiled.ok,
            generatedFiles = generated,
            // エンジンの診断を先に、コンパイラの診断（生成物が原因のコンパイルエラーなど）を後に並べる
            diagnostics = diagnostics + compileDiagnostics.filter { it.severity != CodegenDiagnostic.Severity.Logging },
            messages = listOf(rendered, compiled.messages).filter { it.isNotBlank() }.joinToString("\n"),
            outputDirectory = if (compiled.ok) compiled.outputDirectory else null,
            classpath = classpath,
        )
    }

    /**
     * kct（kotlin-compiler-embeddable）を AA（*-for-ide）と分けた ClassLoader で動かす（ADR-061）。
     * 両者は org.jetbrains.kotlin.cli.* などを同じ名前で持ち、同じ ClassLoader では片方が壊れるため。
     * kct の型はこちらの ClassLoader から見えないので、リフレクションで呼ぶ。
     */
    private object IsolatedKct {
        class Compiled(
            val ok: Boolean,
            val messages: String,
            val diagnostics: List<Pair<String, String>>,
            val outputDirectory: File,
        )

        private val loader: ClassLoader by lazy {
            val entries = hostClasspath().filter { isolatable(it) }
            java.net.URLClassLoader(entries.map { it.toURI().toURL() }.toTypedArray(), ClassLoader.getPlatformClassLoader())
        }

        /** AA と IntelliJ Platform（再配置されていないもの）を除く。kct と embeddable と、その依存だけが残る。 */
        private fun isolatable(entry: File): Boolean {
            if (entry.isDirectory) return true
            if ("-for-ide" in entry.name) return false
            return runCatching {
                java.util.zip.ZipFile(entry).use { zip ->
                    zip.getEntry("com/intellij/") == null &&
                        zip.entries().asSequence().none {
                            it.name.startsWith("com/intellij/") ||
                                it.name.startsWith("org/jetbrains/kotlin/analysis/")
                        }
                }
            }.getOrDefault(true)
        }

        fun compile(
            workingDir: File,
            sources: List<File>,
            classpath: List<File>,
            languageVersion: String?,
        ): Compiled {
            val kctPackage = "me.tbsten.analysis.code.gen.testing.kct"
            val compilationClass = Class.forName("$kctPackage.KotlinCompilation", true, loader)
            val sourceFileClass = Class.forName("$kctPackage.SourceFile", true, loader)
            val companion = sourceFileClass.getField("Companion").get(null)
            val fromPath = companion.javaClass.getMethod("fromPath", File::class.java)
            val compilation = compilationClass.getDeclaredConstructor().newInstance()
            val output = ByteArrayOutputStream()

            fun set(
                name: String,
                type: Class<*>,
                value: Any?,
            ) = compilationClass.getMethod(name, type).invoke(compilation, value)
            set("setWorkingDir", File::class.java, workingDir)
            set("setSources", List::class.java, sources.map { fromPath.invoke(companion, it) })
            // 生成物から見える classpath は明示する（isolated な ClassLoader の classpath を継がない）
            set("setInheritClassPath", Boolean::class.javaPrimitiveType!!, false)
            set("setClasspaths", List::class.java, classpath)
            set("setLanguageVersion", String::class.java, languageVersion)
            set("setMessageOutputStream", java.io.OutputStream::class.java, output)
            val result = compilationClass.getMethod("compile").invoke(compilation)

            fun get(name: String): Any? = result.javaClass.getMethod(name).invoke(result)
            val diagnostics =
                (get("getDiagnosticMessages") as List<*>).map { d ->
                    val severity =
                        d!!
                            .javaClass
                            .getMethod("getSeverity")
                            .invoke(d)
                            .toString()
                    severity to
                        d.javaClass
                            .getMethod("getMessage")
                            .invoke(d)
                            .toString()
                }
            return Compiled(
                ok = get("getExitCode").toString() == "OK",
                messages = get("getMessages") as String,
                diagnostics = diagnostics,
                outputDirectory = get("getOutputDirectory") as File,
            )
        }
    }

    private fun model(
        spec: CodegenTestSpec,
        root: File,
        sourceSets: List<TestSourceSet>,
        classpath: List<File>,
    ): ProjectModel {
        val names = sourceSets.map { it.name }.toSet()
        val ssModels =
            sourceSets.map { ss ->
                SourceSetModel(
                    name = ss.name,
                    platform = platformOf(ss.name),
                    kotlinSourceRoots = listOf("src/${ss.name}/kotlin"),
                    javaSourceRoots = listOf("src/${ss.name}/java"),
                    dependsOn = ss.dependsOn.filter { it in names },
                    upstreamGeneratedRoots = closure(ss.name, sourceSets).drop(1).map { "${outputBase(it)}/kotlin" },
                )
            }
        // 依存の末端（どこからも dependsOn されない SourceSet）ごとに Compilation を 1 つ作る
        val leaves = sourceSets.filter { s -> sourceSets.none { s.name in it.dependsOn } }
        val compilations =
            leaves.map { leaf ->
                CompilationModel(
                    name = leaf.name,
                    targetName = if (leaf.name == CodegenTestSpec.MAIN) "" else leaf.name.removeSuffix("Main").removeSuffix("Test"),
                    compilationName = if (leaf.name.endsWith("Test")) "test" else "main",
                    platform = platformOf(leaf.name).takeIf { it != PlatformKind.Common } ?: PlatformKind.Jvm,
                    defaultSourceSet = leaf.name,
                    sourceSets = closure(leaf.name, sourceSets),
                    classpath = classpath.map { it.absolutePath },
                    friendPaths = emptyList(),
                    jdkHome = System.getProperty("java.home"),
                    compilerOptions = CompilerOptionsModel(spec.languageVersion, null, emptyList(), emptyList(), "17"),
                )
            }
        val units =
            sourceSets.map { ss ->
                ExecutionUnitModel(
                    name = ss.name,
                    kind = UnitKind.SourceSet,
                    target = ss.name,
                    analysisCompilation = compilations.first { ss.name in it.sourceSets }.name,
                    processingScope =
                        if (ss.processingScope == ProcessingScope.IncludeDependsOn) {
                            ProcessingScopeModel.IncludeDependsOn
                        } else {
                            ProcessingScopeModel.Own
                        },
                    // Processor は EngineRequest.extraComponents でインスタンスのまま渡す
                    processors = emptyList(),
                    options = spec.options.toMap(),
                    outputDirs =
                        OutputDirsModel(
                            "${outputBase(ss.name)}/kotlin",
                            "${outputBase(ss.name)}/java",
                            "${outputBase(ss.name)}/resources",
                        ),
                    stagingDir = "build/analysisCodeGen/staging/${ss.name}",
                    cacheDir = "build/analysisCodeGen/caches/${ss.name}",
                )
            }
        val hasKsp = spec.processors.any { it is TestProcessor.Ksp || it is TestProcessor.Jar }
        return ProjectModel(
            schemaVersion = 1,
            rootDir = root.absolutePath,
            sourceSets = ssModels,
            compilations = compilations,
            units = units,
            analysis =
                AnalysisConfig(
                    analysisApiVersion = KotlinVersion.CURRENT.toString(),
                    maxRounds = spec.maxRounds ?: 100,
                    unresolvedDeferred = if (spec.failOnUnresolvedDeferred) DeferredSeverityModel.Error else DeferredSeverityModel.Warning,
                    kspBridge = if (hasKsp) KspBridgeConfig(KspProcessingModeModel.Compatible, kspVersionCheck = false) else null,
                ),
        )
    }

    private fun component(
        index: Int,
        processor: TestProcessor,
    ): ProcessorComponent =
        when (processor) {
            is TestProcessor.Native -> InstanceProcessorComponent(processor.provider.javaClass.name, listOf(processor.provider))
            is TestProcessor.Ksp -> InstanceProcessorComponent(processor.provider.javaClass.name, listOf(processor.provider))
            is TestProcessor.Jar -> ClasspathProcessorComponent("test:jar$index:${processor.file.name}", listOf(processor.file))
        }

    private fun outputBase(sourceSet: String) = "build/generated/analysisCodeGen/$sourceSet"

    private fun dirOf(path: String) = if (path.endsWith(".java")) "java" else "kotlin"

    private fun platformOf(name: String): PlatformKind =
        when {
            name == CodegenTestSpec.MAIN || name == "test" -> PlatformKind.Jvm
            name.startsWith("common") -> PlatformKind.Common
            name.startsWith("jvm") || name.startsWith("android") -> PlatformKind.Jvm
            name.startsWith("js") -> PlatformKind.Js
            name.startsWith("wasm") -> PlatformKind.Wasm
            name.startsWith(
                "ios",
            ) ||
                name.startsWith("macos") ||
                name.startsWith("linux") ||
                name.startsWith("native") -> PlatformKind.Native
            else -> PlatformKind.Jvm
        }

    /** 自分 + dependsOn を遡った上流（自分が先頭）。 */
    private fun closure(
        name: String,
        all: List<TestSourceSet>,
    ): List<String> {
        val result = linkedSetOf<String>()
        val queue = ArrayDeque(listOf(name))
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (result.add(next)) queue.addAll(all.firstOrNull { it.name == next }?.dependsOn.orEmpty())
        }
        return result.filter { n -> all.any { it.name == n } }
    }

    /** 上流の SourceSet を先に流す。 */
    private fun topologicalOrder(all: List<TestSourceSet>): List<TestSourceSet> {
        val done = linkedSetOf<String>()

        fun visit(ss: TestSourceSet) {
            if (ss.name in done) return
            ss.dependsOn.mapNotNull { d -> all.firstOrNull { it.name == d } }.forEach(::visit)
            done += ss.name
        }
        all.forEach(::visit)
        return done.map { n -> all.first { it.name == n } }
    }

    private fun hostClasspath(): List<File> =
        System
            .getProperty("java.class.path")
            .split(File.pathSeparator)
            .filter { it.isNotBlank() }
            .map(::File)
            .filter { it.exists() }

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

    private fun Diagnostic.Severity.toCodegen() =
        when (this) {
            Diagnostic.Severity.Error -> CodegenDiagnostic.Severity.Error
            Diagnostic.Severity.Warning -> CodegenDiagnostic.Severity.Warning
            Diagnostic.Severity.Info -> CodegenDiagnostic.Severity.Info
        }
}

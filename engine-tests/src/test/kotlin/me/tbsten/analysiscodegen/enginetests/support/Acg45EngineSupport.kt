@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysiscodegen.enginetests.support

import me.tbsten.analysiscodegen.analysis.AnalysisEnvironment
import me.tbsten.analysiscodegen.analysis.AnalysisEnvironmentFactory
import me.tbsten.analysiscodegen.analysis.AnalysisInvalidationResult
import me.tbsten.analysiscodegen.analysis.SourceChangeSet
import me.tbsten.analysiscodegen.analysis.StandaloneAnalysisEnvironmentFactory
import me.tbsten.analysiscodegen.engine.AnalysisCodegenEngine
import me.tbsten.analysiscodegen.engine.EngineComponents
import me.tbsten.analysiscodegen.engine.EngineRequest
import me.tbsten.analysiscodegen.engine.EngineResult
import me.tbsten.analysiscodegen.model.AnalysisConfig
import me.tbsten.analysiscodegen.model.CompilationModel
import me.tbsten.analysiscodegen.model.CompilerOptionsModel
import me.tbsten.analysiscodegen.model.DeferredSeverityModel
import me.tbsten.analysiscodegen.model.ExecutionUnitModel
import me.tbsten.analysiscodegen.model.OutputDirsModel
import me.tbsten.analysiscodegen.model.PlatformKind
import me.tbsten.analysiscodegen.model.ProcessingScopeModel
import me.tbsten.analysiscodegen.model.ProcessorModel
import me.tbsten.analysiscodegen.model.ProjectModel
import me.tbsten.analysiscodegen.model.SourceSetModel
import me.tbsten.analysiscodegen.model.UnitKind
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorProvider
import me.tbsten.analysiscodegen.processor.InternalAnalysisCodegenApi
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

// ACG-45 の engine-tests（CCH・MOD・INC-13/15・FAIL-04・MIX・HYB）が使う補助（ADR-034）。
// ProjectModel を手で組み、AnalysisCodegenEngine を直接回す。Gradle を通さないので、エンジンの部品（EngineComponents）を差し替えて観察できる

/** 1 つの SourceSet。[dependsOn] は上流の名前。 */
data class EngineSourceSet(
    val name: String,
    val platform: PlatformKind = PlatformKind.Jvm,
    val dependsOn: List<String> = emptyList(),
)

/**
 * 一時ディレクトリに置いたソースと、Processor の組からなるテスト用プロジェクト。
 *
 * - 既定は kotlin("jvm") 相当（SourceSet `main` 1 つ、実行単位 `main`）
 * - Processor はテストのクラスを ServiceLoader で見つけさせる（[nativeProcessor] / [kspProcessor]）
 */
class EngineProject(
    val sourceSets: List<EngineSourceSet> = listOf(EngineSourceSet("main")),
    val root: File = Files.createTempDirectory("acg-engine").toFile(),
) {
    private val processors = mutableListOf<ProcessorModel>()

    /** 実行単位ごとのオプション。 */
    val options: MutableMap<String, String> = linkedMapOf()

    fun kotlin(
        path: String,
        content: String,
        sourceSet: String = sourceSets.first().name,
    ): File = write("src/$sourceSet/kotlin/$path", content)

    fun java(
        path: String,
        content: String,
        sourceSet: String = sourceSets.first().name,
    ): File = write("src/$sourceSet/java/$path", content)

    fun delete(
        path: String,
        sourceSet: String = sourceSets.first().name,
    ) {
        check(File(root, "src/$sourceSet/kotlin/$path").delete()) { "$path が無い" }
    }

    fun write(
        path: String,
        content: String,
    ): File =
        File(root, path).apply {
            parentFile.mkdirs()
            writeText(content.trimIndent())
        }

    /** Native Processor を足す。[provider] のクラスを `META-INF/services` に書いた classpath を作る。 */
    fun nativeProcessor(provider: KClass<out AnalysisCodegenProcessorProvider>) {
        processors +=
            serviceComponent("native-${provider.simpleName}", AnalysisCodegenProcessorProvider::class.java.name, provider.java.name)
    }

    /** KSP Processor を足す（Bridge で動く）。 */
    fun kspProcessor(providerClassName: String) {
        processors +=
            serviceComponent(
                "ksp-${providerClassName.substringAfterLast('.')}",
                "com.google.devtools.ksp.processing.SymbolProcessorProvider",
                providerClassName,
            )
    }

    private fun serviceComponent(
        id: String,
        service: String,
        impl: String,
    ): ProcessorModel {
        val dir = File(root, "processors/$id").apply { mkdirs() }
        File(dir, "META-INF/services/$service").apply {
            parentFile.mkdirs()
            writeText(impl + "\n")
        }
        return ProcessorModel("test:$id:1.0", listOf(dir.absolutePath) + testClasspath())
    }

    /** 実行単位（SourceSet 単位）の名前。 */
    fun unitName(sourceSet: String = sourceSets.first().name): String = sourceSet

    fun outputDir(sourceSet: String = sourceSets.first().name): File = File(root, "build/generated/analysisCodeGen/$sourceSet")

    /** 生成物（kotlin/java/resources からの相対パス → 中身）。 */
    fun generated(sourceSet: String = sourceSets.first().name): Map<String, String> {
        val dir = outputDir(sourceSet)
        if (!dir.isDirectory) return emptyMap()
        return dir
            .walkTopDown()
            .filter { it.isFile }
            .associate {
                it
                    .relativeTo(
                        dir,
                    ).invariantSeparatorsPath to it.readText()
            }.toSortedMap()
    }

    fun model(): ProjectModel {
        val stdlib = testClasspath().filter { "kotlin-stdlib" in File(it).name }
        val ssModels =
            sourceSets.map { ss ->
                SourceSetModel(
                    name = ss.name,
                    platform = ss.platform,
                    kotlinSourceRoots = listOf("src/${ss.name}/kotlin"),
                    javaSourceRoots = listOf("src/${ss.name}/java"),
                    dependsOn = ss.dependsOn,
                    upstreamGeneratedRoots = ss.dependsOn.map { "build/generated/analysisCodeGen/$it/kotlin" },
                )
            }
        // 依存の末端（どこからも dependsOn されない SourceSet）ごとに Compilation を 1 つ作る
        val leaves = sourceSets.filter { s -> sourceSets.none { s.name in it.dependsOn } }

        fun closure(name: String): List<String> = listOf(name) + sourceSets.first { it.name == name }.dependsOn.flatMap(::closure)
        val compilations =
            leaves.map { leaf ->
                CompilationModel(
                    name = leaf.name,
                    targetName = if (leaf.name == "main") "" else leaf.name.removeSuffix("Main"),
                    compilationName = "main",
                    platform = leaf.platform,
                    defaultSourceSet = leaf.name,
                    sourceSets = closure(leaf.name).distinct(),
                    classpath = stdlib,
                    friendPaths = emptyList(),
                    jdkHome = System.getProperty("java.home"),
                    compilerOptions = CompilerOptionsModel(null, null, emptyList(), emptyList(), "17"),
                )
            }
        val units =
            sourceSets.map { ss ->
                ExecutionUnitModel(
                    name = ss.name,
                    kind = UnitKind.SourceSet,
                    target = ss.name,
                    analysisCompilation = compilations.first { ss.name in it.sourceSets }.name,
                    processingScope = ProcessingScopeModel.Own,
                    processors = processors.toList(),
                    options = options.toMap(),
                    outputDirs =
                        OutputDirsModel(
                            "build/generated/analysisCodeGen/${ss.name}/kotlin",
                            "build/generated/analysisCodeGen/${ss.name}/java",
                            "build/generated/analysisCodeGen/${ss.name}/resources",
                        ),
                    stagingDir = "build/analysisCodeGen/staging/${ss.name}",
                    cacheDir = "build/analysisCodeGen/caches/${ss.name}",
                )
            }
        return ProjectModel(
            schemaVersion = 1,
            rootDir = root.absolutePath,
            sourceSets = ssModels,
            compilations = compilations,
            units = units,
            analysis = AnalysisConfig(KotlinVersion.CURRENT.toString(), 100, DeferredSeverityModel.Error, null),
        )
    }

    /** エンジンを 1 回回す。同じ [engine] を使い回すと、ラウンド間・ビルド間で部品が保たれる。 */
    fun run(
        engine: AnalysisCodegenEngine = AnalysisCodegenEngine(),
        sourceSet: String = sourceSets.first().name,
        forceFull: Boolean = false,
    ): EngineResult = engine.run(EngineRequest(model(), unitName(sourceSet), forceFull))

    companion object {
        fun testClasspath(): List<String> = System.getProperty("java.class.path").split(File.pathSeparator).filter { it.isNotBlank() }
    }
}

/** [delegate] を包み、環境の作成回数と差分反映の結果を記録する Factory（CCH-01・MOD-*）。 */
class RecordingEnvironmentFactory(
    private val delegate: AnalysisEnvironmentFactory = StandaloneAnalysisEnvironmentFactory(),
) : AnalysisEnvironmentFactory {
    val created = AtomicInteger()
    val invalidations = mutableListOf<Pair<SourceChangeSet, AnalysisInvalidationResult>>()

    override fun create(
        model: ProjectModel,
        unitName: String,
    ): AnalysisEnvironment {
        created.incrementAndGet()
        val env = delegate.create(model, unitName)
        return object : AnalysisEnvironment by env {
            override fun applyChanges(changes: SourceChangeSet): AnalysisInvalidationResult =
                env.applyChanges(changes).also {
                    invalidations +=
                        changes to it
                }
        }
    }
}

/** 既定の部品のうち、解析環境の Factory だけを差し替えたエンジン。 */
fun engineWith(factory: AnalysisEnvironmentFactory): AnalysisCodegenEngine {
    val defaults = EngineComponents.default()
    return AnalysisCodegenEngine(EngineComponents(factory, defaults.dirtyFileCalculator, defaults.frontends))
}

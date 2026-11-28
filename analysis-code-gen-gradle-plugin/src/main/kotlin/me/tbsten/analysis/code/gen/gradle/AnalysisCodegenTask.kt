package me.tbsten.analysis.code.gen.gradle

import kotlinx.serialization.json.Json
import me.tbsten.analysis.code.gen.model.AnalysisConfig
import me.tbsten.analysis.code.gen.model.DeferredSeverityModel
import me.tbsten.analysis.code.gen.model.KspBridgeConfig
import me.tbsten.analysis.code.gen.model.KspProcessingModeModel
import me.tbsten.analysis.code.gen.model.ProcessorModel
import me.tbsten.analysis.code.gen.model.ProjectModel
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * 1 実行単位を処理するタスク（ADR-016）。名前は [AnalysisCodegenNaming] で決める。
 *
 * エンジンと AA は [runtimeClasspath] から Worker に読み込み、ビルドスクリプトの classpath には載せない（GRD-21）。
 * ProjectModel は実行時にタスクの入力だけから組み立てる（Configuration Cache のため Project を触らない。ADR-063）。
 */
@CacheableTask
public abstract class AnalysisCodegenTask : DefaultTask() {
    @get:Input
    public abstract val unitName: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val sources: ConfigurableFileCollection

    /** 上流の実行単位の生成物。自分の出力は含めない（GRD-03）。 */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val upstreamGenerated: ConfigurableFileCollection

    @get:CompileClasspath
    public abstract val compileClasspath: ConfigurableFileCollection

    /** friend 依存（テストから main の internal を見る）。 */
    @get:CompileClasspath
    public abstract val friendPaths: ConfigurableFileCollection

    /** Processor の依存（実行単位の Configuration）。空ならタスクは NO-SOURCE（GRD-27）。 */
    @get:Classpath
    @get:SkipWhenEmpty
    public abstract val processorClasspath: ConfigurableFileCollection

    /** analysis-code-gen-processing-engine と AA 一式（`analysisCodeGenRuntime`）。 */
    @get:Classpath
    public abstract val runtimeClasspath: ConfigurableFileCollection

    /**
     * KSP Bridge の実行時 JAR。KSP の Processor が [processorClasspath] にあるときだけ中身が入る（ADR-050）。
     * 判定は実行時の依存解決の結果で行い、KSP の Processor が無ければ Bridge を取得しない。
     */
    @get:Classpath
    public abstract val kspBridgeClasspath: ConfigurableFileCollection

    @get:Input
    public abstract val options: MapProperty<String, String>

    /**
     * SourceSet と Compilation の形（パスは [projectDirectory] からの相対）。クラスパスと Processor は実行時に埋める。
     * JSON で持つのは、入れ子の値をそのまま @Input にでき、Configuration Cache にも載せられるため（ADR-063）。
     */
    @get:Input
    public abstract val modelSkeleton: Property<String>

    @get:Input
    public abstract val analysisApiVersion: Property<String>

    @get:Input
    public abstract val maxRounds: Property<Int>

    @get:Input
    public abstract val unresolvedDeferred: Property<DeferredSeverity>

    @get:Input
    public abstract val kspProcessingMode: Property<KspProcessingMode>

    @get:Input
    public abstract val kspVersionCheck: Property<Boolean>

    @get:OutputDirectory
    public abstract val kotlinOutputDir: DirectoryProperty

    @get:OutputDirectory
    public abstract val javaOutputDir: DirectoryProperty

    @get:OutputDirectory
    public abstract val resourcesOutputDir: DirectoryProperty

    /** staging と増分キャッシュ。Build Cache には入れない（ADR-015）。 */
    @get:Internal
    public abstract val workDir: DirectoryProperty

    /** パスの基準（ProjectModel の rootDir）。絶対パスなのでキャッシュキーに入れない（GCC-04）。 */
    @get:Internal
    public abstract val projectDirectory: DirectoryProperty

    /** Processor の依存グラフ。直接の依存ごとに ProcessorModel を作るために使う。 */
    @get:Internal
    public abstract val processorGraph: Property<ResolvedComponentResult>

    @get:Internal
    public abstract val processorArtifacts: SetProperty<ResolvedArtifactResult>

    @get:Internal
    public abstract val workerIsolation: Property<WorkerIsolation>

    @get:Internal
    public abstract val workerMaxHeapSize: Property<String>

    @get:Internal
    public abstract val workerJvmArgs: ListProperty<String>

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    @TaskAction
    public fun generate(inputChanges: InputChanges) {
        val root = projectDirectory.get().asFile
        val modelJson = MODEL_JSON.encodeToString(ProjectModel.serializer(), buildModel(root))
        // 調べやすいよう、エンジンに渡したモデルを残す（ADR-063）
        val workRoot = workDir.get().asFile.apply { mkdirs() }
        val modelFile = workRoot.resolve("project-model.json").apply { writeText(modelJson) }

        val queue =
            when (workerIsolation.get()) {
                WorkerIsolation.ClassLoader ->
                    workerExecutor.classLoaderIsolation { spec ->
                        spec.classpath.from(runtimeClasspath, kspBridgeClasspath)
                    }
                WorkerIsolation.Process ->
                    workerExecutor.processIsolation { spec ->
                        spec.classpath.from(runtimeClasspath, kspBridgeClasspath)
                        workerMaxHeapSize.orNull?.let { spec.forkOptions.maxHeapSize = it }
                        spec.forkOptions.jvmArgs(workerJvmArgs.get())
                    }
            }
        queue.submit(AnalysisCodegenWorkAction::class.java) { params ->
            params.modelFile.set(modelFile)
            params.unitName.set(unitName)
            params.forceFull.set(!inputChanges.isIncremental)
            params.isolation.set(workerIsolation.get().name)
        }
    }

    private fun buildModel(root: File): ProjectModel {
        val skeleton = MODEL_JSON.decodeFromString(ProjectModel.serializer(), modelSkeleton.get())

        fun rel(file: File): String = file.relativeToOrSelf(root).invariantSeparatorsPath

        fun existing(paths: List<String>): List<String> = paths.filter { root.resolve(it).isDirectory }

        val processors = processorModels(::rel)
        val hasKsp = processorClasspath.files.any { it.hasService(KSP_PROVIDER_SERVICE) }
        return skeleton.copy(
            rootDir = root.absolutePath,
            sourceSets =
                skeleton.sourceSets.map {
                    it.copy(
                        kotlinSourceRoots = existing(it.kotlinSourceRoots),
                        javaSourceRoots = existing(it.javaSourceRoots),
                    )
                },
            compilations =
                skeleton.compilations.map {
                    it.copy(
                        classpath = compileClasspath.files.filter(File::exists).map(::rel),
                        friendPaths = friendPaths.files.filter(File::exists).map(::rel),
                    )
                },
            units = skeleton.units.map { it.copy(processors = processors, options = options.get().toSortedMap()) },
            analysis =
                AnalysisConfig(
                    analysisApiVersion = analysisApiVersion.get(),
                    maxRounds = maxRounds.get(),
                    unresolvedDeferred =
                        when (unresolvedDeferred.get()) {
                            DeferredSeverity.Error -> DeferredSeverityModel.Error
                            DeferredSeverity.Warning -> DeferredSeverityModel.Warning
                        },
                    kspBridge =
                        if (hasKsp) {
                            KspBridgeConfig(
                                processingMode =
                                    when (kspProcessingMode.get()) {
                                        KspProcessingMode.Compatible -> KspProcessingModeModel.Compatible
                                        KspProcessingMode.SourceSet -> KspProcessingModeModel.SourceSet
                                    },
                                kspVersionCheck = kspVersionCheck.get(),
                            )
                        } else {
                            null
                        },
                ),
        )
    }

    /**
     * 直接の依存 1 つを 1 つの ProcessorModel にし、その推移的な依存を classpath に入れる。
     * 依存の JAR（kotlin-stdlib など）を別の component にすると、Provider の無い component として警告されるため（API-14）。
     */
    private fun processorModels(rel: (File) -> String): List<ProcessorModel> {
        val artifacts = processorArtifacts.get().groupBy({ it.id.componentIdentifier }, { it.file })
        return processorGraph
            .get()
            .dependencies
            .filterIsInstance<ResolvedDependencyResult>()
            .map { it.selected }
            .distinctBy { it.id }
            .map { component ->
                val closure = linkedSetOf<ComponentIdentifier>()

                fun visit(c: ResolvedComponentResult) {
                    if (!closure.add(c.id)) return
                    c.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { visit(it.selected) }
                }
                visit(component)
                ProcessorModel(
                    component = component.id.coordinates(),
                    classpath = closure.flatMap { artifacts[it].orEmpty() }.distinct().map(rel),
                )
            }
    }

    internal companion object {
        val MODEL_JSON: Json =
            Json {
                prettyPrint = true
                encodeDefaults = true
            }

        const val KSP_PROVIDER_SERVICE: String = "META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider"

        fun ComponentIdentifier.coordinates(): String =
            when (this) {
                is ModuleComponentIdentifier -> "$group:$module:$version"
                is ProjectComponentIdentifier -> "project($projectPath)"
                else -> displayName
            }

        fun File.hasService(entry: String): Boolean =
            when {
                isDirectory -> resolve(entry).isFile
                isFile && name.endsWith(".jar") -> runCatching { ZipFile(this).use { it.getEntry(entry) != null } }.getOrDefault(false)
                else -> false
            }
    }
}

/** タスク名・出力ディレクトリの命名規則（ADR-016、ADR-049、KMP-23）。 */
public object AnalysisCodegenNaming {
    private const val PREFIX = "analysisCodeGen"

    /** `commonMain` → `analysisCodeGenCommonMainSourceSet`。 */
    public fun sourceSetTaskName(sourceSetName: String): String = PREFIX + sourceSetName.capitalized() + "SourceSet"

    /** (`jvm`, `main`) → `analysisCodeGenJvmMainCompilation`。kotlin("jvm") の target 名は空文字。 */
    public fun compilationTaskName(
        targetName: String,
        compilationName: String,
    ): String = PREFIX + compilationUnitName(targetName, compilationName).capitalized() + "Compilation"

    /** (`jvm`, `main`) → `jvmMain`。 */
    public fun compilationUnitName(
        targetName: String,
        compilationName: String,
    ): String = if (targetName.isEmpty()) compilationName else targetName + compilationName.capitalized()

    /** `commonMain` → `analysisCodeGenCommonMainSourceSet`（Configuration 名。タスク名と同じ形）。 */
    public fun sourceSetConfigurationName(sourceSetName: String): String = sourceSetTaskName(sourceSetName)

    /** (`jvm`, `main`) → `analysisCodeGenJvmMainCompilation`。 */
    public fun compilationConfigurationName(
        targetName: String,
        compilationName: String,
    ): String = compilationTaskName(targetName, compilationName)

    /** `build/generated/analysisCodeGen` からの相対パス（`commonMain`）。 */
    public fun sourceSetOutputDir(sourceSetName: String): String = sourceSetName

    /** `build/generated/analysisCodeGen` からの相対パス（`compilations/jvmMain`）。 */
    public fun compilationOutputDir(unitName: String): String = "compilations/$unitName"

    private fun String.capitalized(): String = replaceFirstChar { it.uppercaseChar() }
}

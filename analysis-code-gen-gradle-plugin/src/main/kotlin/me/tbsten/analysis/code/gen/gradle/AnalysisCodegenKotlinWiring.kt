package me.tbsten.analysis.code.gen.gradle

import me.tbsten.analysis.code.gen.model.AnalysisConfig
import me.tbsten.analysis.code.gen.model.CompilationModel
import me.tbsten.analysis.code.gen.model.CompilerOptionsModel
import me.tbsten.analysis.code.gen.model.DeferredSeverityModel
import me.tbsten.analysis.code.gen.model.ExecutionUnitModel
import me.tbsten.analysis.code.gen.model.OutputDirsModel
import me.tbsten.analysis.code.gen.model.PlatformKind
import me.tbsten.analysis.code.gen.model.ProcessingScopeModel
import me.tbsten.analysis.code.gen.model.ProjectModel
import me.tbsten.analysis.code.gen.model.SourceSetModel
import me.tbsten.analysis.code.gen.model.UnitKind
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool
import java.io.File

/**
 * KGP（と AGP の組み込み Kotlin）の SourceSet / Compilation から実行単位を作り、タスクを登録する（ADR-016、ADR-063）。
 *
 * - 実行単位ごとの Configuration は、Kotlin の SourceSet / Compilation が現れた時点で作る（`dependencies { }` から使えるように）
 * - タスクはプロジェクトの評価が終わってから、Processor のある実行単位だけ登録する（GRD-27）
 * - SourceSet 単位の生成物はその SourceSet に、Compilation 単位の生成物はその Compilation のコンパイルタスクだけに足す（KMP-13）
 */
internal class AnalysisCodegenKotlinWiring(
    private val project: Project,
    private val extension: AnalysisCodegenExtension,
    private val runtime: AnalysisCodegenRuntime,
) {
    private lateinit var kotlin: KotlinProjectExtension
    private var evaluated = false
    private val pending = mutableListOf<() -> Unit>()

    /** SourceSet 名 → その SourceSet 単位のタスク（上流の生成物をたどるため）。 */
    private val sourceSetUnits = mutableMapOf<String, TaskProvider<AnalysisCodegenTask>>()

    private val generatedRoot get() = project.layout.buildDirectory.dir("generated/analysisCodeGen")

    fun wire(kotlin: KotlinProjectExtension) {
        this.kotlin = kotlin
        val root = runtime.processorConfiguration(ROOT_CONFIGURATION)
        root.description = "analysisCodeGen の Processor（ルートの SourceSet: main / commonMain）"

        kotlin.sourceSets.all { sourceSet ->
            extension.sourceSets.maybeCreate(sourceSet.name)
            val configuration = runtime.processorConfiguration(AnalysisCodegenNaming.sourceSetConfigurationName(sourceSet.name))
            addProcessorDependencies(configuration, extension.sourceSets.getByName(sourceSet.name))
            if (sourceSet.name == "main" || sourceSet.name == "commonMain") configuration.extendsFrom(root)
            whenEvaluated { registerSourceSetUnit(sourceSet, configuration) }
        }
        // KMP のターゲットは kotlin { jvm() } を評価するときに増えるので、今あるものではなく all で受ける
        allTargets { target ->
            target.compilations.all { compilation ->
                if (target.isMetadata()) return@all
                val unitName = AnalysisCodegenNaming.compilationUnitName(target.unitTargetName(), compilation.name)
                val spec = extension.compilations.maybeCreate(unitName)
                val configuration =
                    runtime.processorConfiguration(
                        AnalysisCodegenNaming.compilationConfigurationName(target.unitTargetName(), compilation.name),
                    )
                addProcessorDependencies(configuration, spec)
                whenEvaluated { registerCompilationUnit(compilation, unitName, configuration) }
            }
        }
    }

    fun onEvaluated() {
        evaluated = true
        val actions = pending.toList()
        pending.clear()
        actions.forEach { it() }
        warnUnknownSpecs()
    }

    private fun whenEvaluated(action: () -> Unit) {
        if (evaluated) action() else pending += action
    }

    private fun targets(): Collection<KotlinTarget> =
        when (val k = kotlin) {
            is KotlinMultiplatformExtension -> k.targets
            is KotlinSingleTargetExtension<*> -> listOf(k.target)
            else -> emptyList()
        }

    private fun allTargets(action: (KotlinTarget) -> Unit) {
        when (val k = kotlin) {
            is KotlinMultiplatformExtension -> k.targets.all(action)
            is KotlinSingleTargetExtension<*> -> action(k.target)
            else -> Unit
        }
    }

    private fun KotlinTarget.isMetadata(): Boolean = platformType == KotlinPlatformType.common

    /** 単一ターゲット（kotlin("jvm")・Android）は target 名を名前に入れない（`main`、`freeDebug`。ADR-016）。 */
    private fun KotlinTarget.unitTargetName(): String = if (kotlin is KotlinMultiplatformExtension) targetName else ""

    private fun addProcessorDependencies(
        configuration: Configuration,
        spec: AnalysisCodegenUnitSpecBase,
    ) {
        configuration.dependencies.addAllLater(
            spec.processorNotations.map { notations ->
                notations.map { n -> project.dependencies.create(if (n is Provider<*>) n.get()!! else n) }
            },
        )
    }

    private fun warnUnknownSpecs() {
        val known = kotlin.sourceSets.names
        extension.sourceSets
            .filter { it.name !in known && it.processorNotations.get().isNotEmpty() }
            .forEach { project.logger.warn("w: analysisCodeGen: SourceSet '${it.name}' はプロジェクト '${project.path}' に無いので無視する") }
    }

    // --- SourceSet 単位 ---

    private fun registerSourceSetUnit(
        sourceSet: KotlinSourceSet,
        configuration: Configuration,
    ) {
        if (configuration.allDependencies.isEmpty()) return
        val name = sourceSet.name
        val spec = extension.sourceSets.getByName(name)
        val outputRel = AnalysisCodegenNaming.sourceSetOutputDir(name)
        val taskName = AnalysisCodegenNaming.sourceSetTaskName(name)
        val task =
            project.tasks.register(taskName, AnalysisCodegenTask::class.java) { t ->
                t.description = "analysisCodeGen: SourceSet '$name' の Processor を動かす"
                val compilation = analysisCompilationOf(sourceSet)
                configureCommon(t, name, outputRel, "sourceSets/$name", configuration, spec)
                configureAnalysis(t, compilation, ownSourceSet = name)
                t.modelSkeleton.set(
                    skeleton(
                        unit = name,
                        kind = UnitKind.SourceSet,
                        compilation = compilation,
                        scope =
                            when (spec.processingScope.value.getOrElse(ProcessingScope.Own)) {
                                ProcessingScope.Own -> ProcessingScopeModel.Own
                                ProcessingScope.IncludeDependsOn -> ProcessingScopeModel.IncludeDependsOn
                            },
                        outputRel = outputRel,
                        workRel = "sourceSets/$name",
                        ownSourceSet = name,
                    ),
                )
            }
        sourceSetUnits[name] = task
        registerSourceSetOutputs(sourceSet, task, outputRel)
    }

    /**
     * SourceSet の解析に使う Compilation。その SourceSet を既定にする Compilation があればそれ（`jvmMain` → jvm の main）、
     * 共有の SourceSet（`commonMain` など）なら同じ名前の metadata の Compilation（KMP-07）。
     */
    private fun analysisCompilationOf(sourceSet: KotlinSourceSet): KotlinCompilation<*> {
        val all = targets().flatMap { it.compilations }
        return all.firstOrNull { !it.target.isMetadata() && it.defaultSourceSet == sourceSet && it.name == "main" }
            ?: all.firstOrNull { !it.target.isMetadata() && it.defaultSourceSet == sourceSet }
            ?: all.firstOrNull { it.target.isMetadata() && it.name == sourceSet.name }
            ?: all.firstOrNull { sourceSet in it.allKotlinSourceSets }
            ?: error("analysisCodeGen: SourceSet '${sourceSet.name}' を含む Compilation が無い")
    }

    private fun registerSourceSetOutputs(
        sourceSet: KotlinSourceSet,
        task: TaskProvider<AnalysisCodegenTask>,
        outputRel: String,
    ) {
        val kotlinDir = outputDir(outputRel, "kotlin", task)
        val javaDir = outputDir(outputRel, "java", task)
        val resourcesDir = outputDir(outputRel, "resources", task)
        sourceSet.kotlin.srcDir(kotlinDir)
        val javaSourceSet = javaSourceSets()?.findByName(sourceSet.name)
        if (javaSourceSet != null) {
            javaSourceSet.java.srcDir(javaDir)
            javaSourceSet.resources.srcDir(resourcesDir)
        } else {
            sourceSet.resources.srcDir(resourcesDir)
        }
    }

    // --- Compilation 単位 ---

    private fun registerCompilationUnit(
        compilation: KotlinCompilation<*>,
        unitName: String,
        configuration: Configuration,
    ) {
        if (configuration.allDependencies.isEmpty()) return
        val spec = extension.compilations.getByName(unitName)
        val outputRel = AnalysisCodegenNaming.compilationOutputDir(unitName)
        val taskName = AnalysisCodegenNaming.compilationTaskName(compilation.target.unitTargetName(), compilation.name)
        val task =
            project.tasks.register(taskName, AnalysisCodegenTask::class.java) { t ->
                t.description = "analysisCodeGen: Compilation '$unitName' の Processor を動かす"
                configureCommon(t, unitName, outputRel, "compilations/$unitName", configuration, spec)
                configureAnalysis(t, compilation, ownSourceSet = null)
                t.modelSkeleton.set(
                    skeleton(
                        unit = unitName,
                        kind = UnitKind.Compilation,
                        compilation = compilation,
                        scope = ProcessingScopeModel.Own,
                        outputRel = outputRel,
                        workRel = "compilations/$unitName",
                        ownSourceSet = null,
                    ),
                )
            }
        // 生成物はこの Compilation のコンパイルタスクだけが読む。SourceSet に足すと、同じ SourceSet を含むほかの Compilation にも漏れる（KMP-13）
        val kotlinDir = outputDir(outputRel, "kotlin", task)
        compilation.compileTaskProvider.configure { compile ->
            (compile as? KotlinCompileTool)?.source(kotlinDir)
        }
        val resourcesDir = outputDir(outputRel, "resources", task)
        (compilation as? KotlinCompilationWithResources<*>)?.processResourcesTaskName?.let { name ->
            if (name in project.tasks.names) {
                project.tasks.named(name, ProcessResources::class.java).configure { it.from(resourcesDir) }
            }
        }
    }

    // --- タスクの設定 ---

    private fun configureCommon(
        t: AnalysisCodegenTask,
        unitName: String,
        outputRel: String,
        workRel: String,
        configuration: Configuration,
        spec: AnalysisCodegenUnitSpecBase,
    ) {
        t.group = "analysisCodeGen"
        t.unitName.set(unitName)
        t.projectDirectory.set(project.layout.projectDirectory)
        t.kotlinOutputDir.set(generatedRoot.map { it.dir("$outputRel/kotlin") })
        t.javaOutputDir.set(generatedRoot.map { it.dir("$outputRel/java") })
        t.resourcesOutputDir.set(generatedRoot.map { it.dir("$outputRel/resources") })
        t.workDir.set(project.layout.buildDirectory.dir("analysisCodeGen/$workRel"))

        // オプション: グローバル < 実行単位（ADR-016）
        t.options.putAll(extension.args)
        t.options.putAll(spec.args)

        t.processorClasspath.from(configuration)
        t.processorGraph.set(configuration.incoming.resolutionResult.rootComponent)
        t.processorArtifacts.set(configuration.incoming.artifacts.resolvedArtifacts)
        t.runtimeClasspath.from(runtime.runtime)
        val bridge = runtime.kspBridgeRuntime
        t.kspBridgeClasspath.from(
            configuration.incoming.artifacts.resolvedArtifacts.map { artifacts ->
                if (artifacts.any { with(AnalysisCodegenTask) { it.file.hasService(AnalysisCodegenTask.KSP_PROVIDER_SERVICE) } }) {
                    bridge.incoming.files.files
                } else {
                    emptySet()
                }
            },
        )

        t.analysisApiVersion.set(runtime.analysisApiVersion)
        t.maxRounds.set(extension.maxRounds)
        t.unresolvedDeferred.set(extension.unresolvedDeferred)
        t.kspProcessingMode.set(extension.kspBridge.processingMode)
        t.kspVersionCheck.set(extension.kspBridge.kspVersionCheck)
        t.workerIsolation.set(extension.worker.isolation)
        t.workerMaxHeapSize.set(extension.worker.maxHeapSize)
        t.workerJvmArgs.set(extension.worker.jvmArgs)
    }

    /** 解析の入力: ソース・上流の生成物・クラスパス。 */
    private fun configureAnalysis(
        t: AnalysisCodegenTask,
        compilation: KotlinCompilation<*>,
        ownSourceSet: String?,
    ) {
        val closure = compilation.allKotlinSourceSets
        val dirs = closure.flatMap { kotlinRoots(it) + javaRoots(it) }.distinct()
        t.sources.from(project.files(dirs).asFileTree.matching { it.include("**/*.kt", "**/*.java") })
        closure
            .filter { it.name != ownSourceSet }
            .mapNotNull { sourceSetUnits[it.name] }
            .forEach { upstream ->
                t.upstreamGenerated.from(upstream.flatMap { it.kotlinOutputDir }, upstream.flatMap { it.javaOutputDir })
            }
        t.compileClasspath.from(compileDependencies(compilation))
        t.compileClasspath.from(androidBootClasspath(compilation))
        t.friendPaths.from(compilation.associatedCompilations.map { it.output.classesDirs })
        jvmLauncher(compilation)?.let(t.javaLauncher::convention)
    }

    /**
     * JVM の Compilation が使う JDK。java の toolchain（kotlin { jvmToolchain() } も同じものを設定する）に従い、
     * 指定が無ければ Gradle を動かしている JDK になる。Android は android.jar を使うので渡さない（GRD-08）。
     */
    private fun jvmLauncher(compilation: KotlinCompilation<*>): Provider<JavaLauncher>? {
        if (compilation.platformType != KotlinPlatformType.jvm) return null
        val toolchains = project.extensions.findByType(JavaToolchainService::class.java) ?: return null
        val java = project.extensions.findByType(JavaPluginExtension::class.java)
        return if (java != null) toolchains.launcherFor(java.toolchain) else toolchains.launcherFor { }
    }

    /**
     * Compilation のコンパイルクラスパス。Android は依存のモジュールが AAR・クラスの JAR など複数の形を出すので、
     * Configuration をそのまま解決すると形を選べずに失敗する。AGP のコンパイルと同じくクラスの JAR（`android-classes-jar`）を選ぶ（GRD-12）
     */
    private fun compileDependencies(compilation: KotlinCompilation<*>): FileCollection {
        if (compilation.platformType != KotlinPlatformType.androidJvm) return compilation.compileDependencyFiles
        val configuration =
            project.configurations.findByName(compilation.compileDependencyConfigurationName)
                ?: return compilation.compileDependencyFiles
        return configuration.incoming
            .artifactView { view ->
                view.attributes { it.attribute(ARTIFACT_TYPE, "android-classes-jar") }
            }.files
    }

    /**
     * Android の boot classpath（android.jar）。KGP の compileDependencyFiles には入らないことがあるので、
     * AGP の `androidComponents.sdkComponents.bootClasspath` から足す（AGP の API には依存しないようリフレクションで読む。GRD-12）。
     */
    private fun androidBootClasspath(compilation: KotlinCompilation<*>): FileCollection {
        if (compilation.platformType != KotlinPlatformType.androidJvm) return project.files()
        val components = project.extensions.findByName("androidComponents") ?: return project.files()
        val bootClasspath =
            runCatching {
                val sdk = components.javaClass.getMethod("getSdkComponents").invoke(components)
                sdk.javaClass.getMethod("getBootClasspath").invoke(sdk) as Provider<*>
            }.getOrNull() ?: return project.files()
        return project.files(bootClasspath)
    }

    // --- ProjectModel の骨格 ---

    private fun skeleton(
        unit: String,
        kind: UnitKind,
        compilation: KotlinCompilation<*>,
        scope: ProcessingScopeModel,
        outputRel: String,
        workRel: String,
        ownSourceSet: String?,
    ): String {
        val compilationName = AnalysisCodegenNaming.compilationUnitName(compilation.target.targetNameForModel(), compilation.name)
        val sourceSets =
            compilation.allKotlinSourceSets.sortedBy { it.name }.map { s ->
                val javaRoots = javaRoots(s)
                SourceSetModel(
                    name = s.name,
                    platform = platformOf(s, compilation),
                    kotlinSourceRoots = (kotlinRoots(s) - javaRoots.toSet()).map(::rel),
                    javaSourceRoots = javaRoots.map(::rel),
                    dependsOn = s.dependsOn.map { it.name }.sorted(),
                    upstreamGeneratedRoots =
                        if (s.name != ownSourceSet && s.name in sourceSetUnits) {
                            val dir = AnalysisCodegenNaming.sourceSetOutputDir(s.name)
                            listOf("$GENERATED/$dir/kotlin", "$GENERATED/$dir/java")
                        } else {
                            emptyList()
                        },
                )
            }
        val compilationModel =
            CompilationModel(
                name = compilationName,
                targetName = compilation.target.targetName,
                compilationName = compilation.name,
                platform = compilation.platformType.toModel(),
                defaultSourceSet = compilation.defaultSourceSet.name,
                sourceSets = compilation.allKotlinSourceSets.map { it.name }.sorted(),
                classpath = emptyList(),
                friendPaths = emptyList(),
                jdkHome = null,
                // M1 は言語の設定を渡さない（GRD-06・07 は M2）。コンパイルタスクを触るとタスクの依存が循環しやすいため（ADR-063）
                compilerOptions = CompilerOptionsModel(null, null, emptyList(), emptyList(), null),
            )
        val unitModel =
            ExecutionUnitModel(
                name = unit,
                kind = kind,
                target = unit,
                analysisCompilation = compilationName,
                processingScope = scope,
                processors = emptyList(),
                options = emptyMap(),
                outputDirs =
                    OutputDirsModel(
                        kotlin = "$GENERATED/$outputRel/kotlin",
                        java = "$GENERATED/$outputRel/java",
                        resources = "$GENERATED/$outputRel/resources",
                    ),
                stagingDir = "$buildDirRel/analysisCodeGen/$workRel/staging",
                cacheDir = "$buildDirRel/analysisCodeGen/$workRel/caches",
            )
        val model =
            ProjectModel(
                schemaVersion = 1,
                rootDir = ".",
                sourceSets = sourceSets,
                compilations = listOf(compilationModel),
                units = listOf(unitModel),
                analysis = AnalysisConfig("", 0, DeferredSeverityModel.Error, null),
            )
        return AnalysisCodegenTask.MODEL_JSON.encodeToString(ProjectModel.serializer(), model)
    }

    private fun KotlinTarget.targetNameForModel(): String = if (isMetadata()) "" else unitTargetName()

    private fun platformOf(
        sourceSet: KotlinSourceSet,
        compilation: KotlinCompilation<*>,
    ): PlatformKind {
        if (sourceSet == compilation.defaultSourceSet) return compilation.platformType.toModel()
        val platforms =
            targets()
                .filterNot { it.isMetadata() }
                .flatMap { it.compilations }
                .filter { sourceSet in it.allKotlinSourceSets }
                .map { it.platformType }
                .toSet()
        return platforms.singleOrNull()?.toModel() ?: PlatformKind.Common
    }

    private fun KotlinPlatformType.toModel(): PlatformKind =
        when (this) {
            KotlinPlatformType.common -> PlatformKind.Common
            KotlinPlatformType.jvm -> PlatformKind.Jvm
            KotlinPlatformType.androidJvm -> PlatformKind.Android
            KotlinPlatformType.js -> PlatformKind.Js
            KotlinPlatformType.native -> PlatformKind.Native
            KotlinPlatformType.wasm -> PlatformKind.Wasm
        }

    // --- パス ---

    private val buildDirRel: String get() =
        rel(
            project.layout.buildDirectory
                .get()
                .asFile,
        )

    private val GENERATED: String get() = "$buildDirRel/generated/analysisCodeGen"

    private fun rel(file: File): String = file.relativeToOrSelf(project.projectDir).invariantSeparatorsPath

    /** 自分たちの生成物（`build/generated/analysisCodeGen`）は SourceSet のソースとして数えない（GRD-03）。 */
    private fun isGenerated(dir: File): Boolean = dir.startsWith(generatedRoot.get().asFile)

    private fun kotlinRoots(sourceSet: KotlinSourceSet): List<File> =
        sourceSet.kotlin.srcDirs
            .filterNot(::isGenerated)
            .sorted()

    private fun javaRoots(sourceSet: KotlinSourceSet): List<File> =
        javaSourceSets()
            ?.findByName(sourceSet.name)
            ?.java
            ?.srcDirs
            ?.filterNot(::isGenerated)
            ?.sorted()
            .orEmpty()

    private fun javaSourceSets(): SourceSetContainer? =
        if (kotlin is KotlinMultiplatformExtension) null else project.extensions.findByType(SourceSetContainer::class.java)

    /** タスクの出力ディレクトリを、タスクへの依存つきの FileCollection にする（SourceSet に足すと下流のタスクが依存する）。 */
    private fun outputDir(
        outputRel: String,
        kind: String,
        task: TaskProvider<AnalysisCodegenTask>,
    ): FileCollection = project.files(generatedRoot.map { it.dir("$outputRel/$kind") }).builtBy(task)

    companion object {
        private const val ROOT_CONFIGURATION = "analysisCodeGen"
        private val ARTIFACT_TYPE: Attribute<String> = Attribute.of("artifactType", String::class.java)

        /** プロジェクトに Kotlin の拡張（`kotlin { }`）があれば配線して返す。無ければ null。 */
        fun wireIfKotlin(
            project: Project,
            extension: AnalysisCodegenExtension,
        ): AnalysisCodegenKotlinWiring? {
            val kotlin = project.extensions.findByType(KotlinProjectExtension::class.java) ?: return null
            return AnalysisCodegenKotlinWiring(project, extension, AnalysisCodegenRuntime(project, extension)).also { it.wire(kotlin) }
        }
    }
}

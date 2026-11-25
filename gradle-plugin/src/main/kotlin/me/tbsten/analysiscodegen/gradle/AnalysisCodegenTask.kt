package me.tbsten.analysiscodegen.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * 1 実行単位を処理するタスク（ADR-016）。名前は [AnalysisCodegenNaming] で決める。
 *
 * エンジンと AA は [runtimeClasspath] から Worker に読み込み、ビルドスクリプトの classpath には載せない（GRD-21）。
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

    @get:Classpath
    public abstract val compileClasspath: ConfigurableFileCollection

    @get:Classpath
    public abstract val processorClasspath: ConfigurableFileCollection

    /** processing-engine と AA 一式（`analysisCodeGenRuntime`）。 */
    @get:Classpath
    public abstract val runtimeClasspath: ConfigurableFileCollection

    @get:Input
    public abstract val options: MapProperty<String, String>

    @get:OutputDirectory
    public abstract val kotlinOutputDir: DirectoryProperty

    @get:OutputDirectory
    public abstract val javaOutputDir: DirectoryProperty

    @get:OutputDirectory
    public abstract val resourcesOutputDir: DirectoryProperty

    /** staging と増分キャッシュ。Build Cache には入れない（ADR-015）。 */
    @get:Internal
    public abstract val workDir: DirectoryProperty

    @TaskAction
    public fun generate(): Unit = TODO()
}

/** タスク名・出力ディレクトリの命名規則（ADR-016、KMP-23）。 */
public object AnalysisCodegenNaming {
    /** `commonMain` → `analysisCodeGenCommonMain`。 */
    public fun sourceSetTaskName(sourceSetName: String): String = TODO()

    /** (`jvm`, `main`) → `analysisCodeGenJvmMainCompilation`。kotlin("jvm") の target 名は空文字。 */
    public fun compilationTaskName(targetName: String, compilationName: String): String = TODO()

    /** (`jvm`, `main`) → `jvmMain`。 */
    public fun compilationUnitName(targetName: String, compilationName: String): String = TODO()

    /** `commonMain` → `analysisCodeGenCommonMain`（Configuration 名。タスク名と同じ形）。 */
    public fun sourceSetConfigurationName(sourceSetName: String): String = TODO()

    /** (`jvm`, `main`) → `analysisCodeGenJvmMainCompilation`。 */
    public fun compilationConfigurationName(targetName: String, compilationName: String): String = TODO()

    /** `build/generated/analysisCodeGen/<SourceSet>` からの相対パス。 */
    public fun sourceSetOutputDir(sourceSetName: String): String = TODO()

    /** `build/generated/analysisCodeGen/compilations/<Name>` からの相対パス。 */
    public fun compilationOutputDir(unitName: String): String = TODO()
}

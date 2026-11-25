package me.tbsten.analysiscodegen.model

import kotlinx.serialization.Serializable

/**
 * Gradle にも AA にも依存しないプロジェクトモデル（ADR-012）。
 *
 * analysis-code-gen-gradle-plugin が KGP / AGP から組み立てて Worker に渡す。CLI は同じ形の JSON を読む（CLI-01・02）。
 * パスはすべて [rootDir] からの相対パス（GCC-04、DET-03）。
 */
@Serializable
public data class ProjectModel(
    /** JSON の形式番号。互換性を壊す変更で上げる。 */
    val schemaVersion: Int,
    val rootDir: String,
    val sourceSets: List<SourceSetModel>,
    val compilations: List<CompilationModel>,
    val units: List<ExecutionUnitModel>,
    val analysis: AnalysisConfig,
)

@Serializable
public data class SourceSetModel(
    val name: String,
    val platform: PlatformKind,
    val kotlinSourceRoots: List<String>,
    val javaSourceRoots: List<String>,
    /** `dependsOn` の直接の上流の名前。 */
    val dependsOn: List<String>,
    /** 上流の実行単位が生成したディレクトリ。解析対象に含める。 */
    val upstreamGeneratedRoots: List<String>,
)

@Serializable
public data class CompilationModel(
    /** 実行単位の名前（`<target><Compilation>`）。 */
    val name: String,
    val targetName: String,
    val compilationName: String,
    val platform: PlatformKind,
    val defaultSourceSet: String,
    val sourceSets: List<String>,
    /** コンパイルクラスパス（JAR / klib）。 */
    val classpath: List<String>,
    /** friend 依存（テストから main の internal を見る）。 */
    val friendPaths: List<String>,
    val jdkHome: String?,
    val compilerOptions: CompilerOptionsModel,
)

@Serializable
public data class CompilerOptionsModel(
    val languageVersion: String?,
    val apiVersion: String?,
    val optIns: List<String>,
    val freeArgs: List<String>,
    val jvmTarget: String?,
)

@Serializable
public enum class PlatformKind { Common, Jvm, Android, Native, Js, Wasm }

@Serializable
public enum class UnitKind { SourceSet, Compilation }

@Serializable
public enum class ProcessingScopeModel { Own, IncludeDependsOn }

@Serializable
public data class ExecutionUnitModel(
    val name: String,
    val kind: UnitKind,
    /** kind が SourceSet なら SourceSet 名、Compilation なら Compilation の名前。 */
    val target: String,
    /** 解析に使う Compilation（SourceSet 単位でクラスパスを決めるため）。 */
    val analysisCompilation: String,
    val processingScope: ProcessingScopeModel,
    val processors: List<ProcessorModel>,
    /** グローバルとスコープ付きを合成した後のオプション（ADR-016）。 */
    val options: Map<String, String>,
    val outputDirs: OutputDirsModel,
    val stagingDir: String,
    val cacheDir: String,
)

@Serializable
public data class ProcessorModel(
    /** Gradle の component 座標（`group:name:version`、または `project(:path)`）。 */
    val component: String,
    val classpath: List<String>,
)

@Serializable
public data class OutputDirsModel(
    val kotlin: String,
    val java: String,
    val resources: String,
)

@Serializable
public enum class DeferredSeverityModel { Error, Warning }

@Serializable
public data class AnalysisConfig(
    /** 実行する AA のバージョン（ADR-011）。 */
    val analysisApiVersion: String,
    val maxRounds: Int,
    val unresolvedDeferred: DeferredSeverityModel,
    /** Bridge の設定。Bridge を適用していなければ null。 */
    val kspBridge: KspBridgeConfig?,
)

@Serializable
public enum class KspProcessingModeModel { Compatible, SourceSet }

@Serializable
public data class KspBridgeConfig(
    val processingMode: KspProcessingModeModel,
    val kspVersionCheck: Boolean,
)

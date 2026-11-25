package me.tbsten.analysiscodegen.differential

/** 三者差分の 3 つの実行系（pbt-differential-design.md 1 章）。 */
public enum class EngineKind(public val label: String) {
    /** KSP2 + 元の Processor。基準実装（期待値）。 */
    K("KSP2"),

    /** analysisCodeGen + Bridge + 元の Processor JAR。 */
    B("Bridge"),

    /** analysisCodeGen + Native 移植版。 */
    N("Native"),
}

/** 1 つの実行系で 1 回動かした結果。 */
public sealed interface EngineRun {
    public val engine: EngineKind

    /** 動いた。成否（ビルドが通ったか）は [output] の [RunOutput.success]。 */
    public data class Completed(
        override val engine: EngineKind,
        val output: RunOutput,
        val metrics: RunMetrics,
    ) : EngineRun

    /** この実行系では動かせない（未実装、その Processor の版が無い など）。比較は飛ばす。 */
    public data class Unavailable(
        override val engine: EngineKind,
        val reason: String,
    ) : EngineRun
}

/**
 * 計測口（ADR-023）。ベンチマークと毎時の試行数の調整（ADR-022）が読む。
 *
 * [cacheHit] が true のとき [durationMillis] はキャッシュを読んだ時間で、実際に動かした時間ではない。
 */
public data class RunMetrics(
    val durationMillis: Long,
    val cacheHit: Boolean = false,
)

/**
 * 1 回の実行で集めた出力（ADR-023）。比べる前に [Normalizer] を通す。
 *
 * - [files]: 生成物。キーは出力先ルートからの相対パスで `kotlin/…`・`java/…`・`resources/…` のどれかで始まる
 * - [diagnostics]: コンパイラと Processor の Logger の診断（並びは出た順）
 * - [rounds]: ラウンドごとの生成ファイル。測れない実行系（今の K）は null で、比較は飛ばす
 * - [roots]: この実行で使った絶対パス（作業ディレクトリなど）。正規化でプレースホルダに置き換える
 */
public data class RunOutput(
    val success: Boolean,
    val files: Map<String, String>,
    val diagnostics: List<DiagnosticRecord>,
    val rounds: List<RoundRecord>? = null,
    val roots: List<String> = emptyList(),
) {
    /** 生成ソース（Kotlin と Java）。 */
    val sources: Map<String, String> get() = files.filterKeys { it.startsWith("kotlin/") || it.startsWith("java/") }

    /** 生成リソース（`META-INF/services` など）。 */
    val resources: Map<String, String> get() = files.filterKeys { !it.startsWith("kotlin/") && !it.startsWith("java/") }

    /** Processor の Logger が出したもの（KSP2 では `[ksp]` で始まる）。 */
    val logs: List<DiagnosticRecord> get() = diagnostics.filter { it.fromProcessor }
}

/**
 * 診断 1 件。[location] は正規化の後は `相対パス:行`（列は捨てる）。
 * [fromProcessor] は Processor の Logger から出たものか（コンパイラの診断なら false）。
 */
public data class DiagnosticRecord(
    val severity: Severity,
    val message: String,
    val location: String? = null,
    val fromProcessor: Boolean = false,
) {
    public enum class Severity { Error, Warning, Info, Logging }
}

/** ラウンド 1 回で生成されたファイル（相対パス、並べたもの）。 */
public data class RoundRecord(val index: Int, val generatedFiles: List<String>)

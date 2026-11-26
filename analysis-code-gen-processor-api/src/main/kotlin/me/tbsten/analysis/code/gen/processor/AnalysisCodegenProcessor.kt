package me.tbsten.analysis.code.gen.processor

/**
 * Native Processor の入口。`META-INF/services/me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorProvider`
 * に実装クラス名を書くと ServiceLoader で見つかる。
 *
 * [create] は実行単位（1 タスク実行）ごとに 1 回だけ呼ばれる（ADR-013、API-01・12）。
 */
public fun interface AnalysisCodegenProcessorProvider {
    public fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor

    /**
     * オプション [key] の値が前回から変わったとき、どこまで作り直すかを宣言する（ADR-051）。
     *
     * 宣言しないキーは [OptionImpact.Full]（全件処理）。変わったキーのうち 1 つでも `Full` なら全件、
     * `Incremental` だけなら増分のまま、`None` だけなら変化を無視する。キーごとに判定する。
     */
    public fun optionImpact(key: String): OptionImpact = OptionImpact.Full
}

/**
 * オプションが変わったときの影響の大きさ（ADR-051）。[AnalysisCodegenProcessorProvider.optionImpact] が返す。
 */
public enum class OptionImpact {
    /** 全件処理に戻す（既定）。 */
    Full,

    /**
     * 増分のまま。全ファイルを dirty にしない。そのオプションを読む生成物の作り直しは、
     * Processor が [Dependencies] で表す。
     */
    Incremental,

    /** 影響なし。変わっても無視する。 */
    None,
}

/**
 * Processor を作るときに渡す、実行単位ごとの環境。
 */
public interface AnalysisCodegenProcessorEnvironment {
    /** グローバル指定と実行単位の指定を合成したオプション（ADR-016）。`analysisCodeGen.` で始まるキーは含まない。 */
    public val options: Map<String, String>

    public val logger: CodegenLogger

    /** この Processor が動く実行単位。 */
    public val executionUnit: ExecutionUnit

    /** 実行中の Analysis API（= 利用者の Kotlin）のバージョン。例: `2.4.10`。 */
    public val kotlinVersion: String

    /** analysisCodeGen 本体のバージョン。 */
    public val frameworkVersion: String
}

/**
 * Native Processor。
 *
 * - [process] は各ラウンドで 1 回ずつ呼ばれる。同じインスタンスを全ラウンドで使う
 * - 全ラウンドが成功したら [finish] を 1 回、エラー（`logger.error` か例外）があったら代わりに [onError] を 1 回呼ぶ
 * - [finish] と [onError] の中では生成できない
 *
 * ADR-013・014。
 */
public interface AnalysisCodegenProcessor {
    public fun process(context: AnalysisCodegenRoundContext)

    public fun finish() {}

    public fun onError() {}
}

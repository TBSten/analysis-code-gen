package me.tbsten.analysiscodegen.processor

/**
 * processor-api の互換性番号。互換性を壊す変更をしたときだけ上げる。
 *
 * Processor の呼び出しで LinkageError が出たとき、診断にこの番号を出す（ADR-013、VER-02）。
 */
public object AnalysisCodegenApiVersion {
    public const val CURRENT: Int = 1
}

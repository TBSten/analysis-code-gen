package me.tbsten.analysiscodegen.testing

/**
 * テスト対象の Processor を、どの実装で動かすか（ADR-021）。
 *
 * 同じテストを [Ksp2] と [AnalysisCodegen] の両方で流すと、三者差分（ADR-023）の K と B / N を比べられる。
 */
public enum class TestEngine {
    /** 公式の KSP2（kct fork 経由で同じプロセス内で動かす）。KSP Processor だけを動かせる。差分の基準（K）。 */
    Ksp2,

    /**
     * analysisCodeGen のエンジン。Native Processor と、Bridge 経由の KSP Processor を動かす。
     * エンジンは P2 で実装する。それまでは [NotImplementedError] を投げる。
     */
    AnalysisCodegen,
}

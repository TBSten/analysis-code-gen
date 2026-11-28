package me.tbsten.analysis.code.gen.port.cream

import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessor
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorEnvironment
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorProvider

/**
 * cream.kt（TBSten/cream、ADR-019）の Native 書き直し版の入口。
 *
 * KSP 版（`me.tbsten.cream.ksp.CreamSymbolProcessorProvider`）を読んで、AA で書き直したもの。M1 の範囲は
 * `@CopyTo`・`@CopyFrom`・`@CopyToChildren` と、関数名のオプション 3 つ（ADR-071）。範囲外の注釈・引数・オプションは、
 * 黙って違うコードを作らないよう error にする。
 */
public class CreamNativeProcessorProvider : AnalysisCodegenProcessorProvider {
    override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor =
        CreamNativeProcessor(environment.options)
}

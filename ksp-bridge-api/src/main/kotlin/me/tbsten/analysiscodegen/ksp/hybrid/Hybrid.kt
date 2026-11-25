@file:OptIn(ExperimentalAnalysisCodegenApi::class)

package me.tbsten.analysiscodegen.ksp.hybrid

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import me.tbsten.analysiscodegen.processor.ExperimentalAnalysisCodegenApi
import me.tbsten.analysiscodegen.processor.InternalAnalysisCodegenApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtFile

/**
 * Hybrid Processor の API（ADR-017）。KSP Processor の中から、必要な箇所だけ AA / PSI を使う。
 *
 * - スコープの中だけで `KaSymbol` / `KaType` を使う。戻り値で外に出さない（HYB-03）
 * - 素の KSP では [HybridNotSupportedException]。[isAnalysisCodegenBridge] で分岐できる（HYB-04）
 * - 1:1 に対応しないものは [UnsupportedHybridConversionException]（HYB-02）
 * - ここで読んだファイルは KSP の `Dependencies` に明示する（HYB-05）
 */
@ExperimentalAnalysisCodegenApi
public fun <R> KSDeclaration.withAnalysis(block: KaSession.(KaDeclarationSymbol) -> R): R = TODO()

@ExperimentalAnalysisCodegenApi
public fun <R> KSType.withAnalysis(block: KaSession.(KaType) -> R): R = TODO()

@ExperimentalAnalysisCodegenApi
public fun <R> KSFile.withAnalysis(block: KaSession.(KtFile) -> R): R = TODO()

/** analysisCodeGen の KSP Bridge 上で動いているか。 */
@ExperimentalAnalysisCodegenApi
public val Resolver.isAnalysisCodegenBridge: Boolean get() = TODO()

@ExperimentalAnalysisCodegenApi
public class HybridNotSupportedException(message: String) : UnsupportedOperationException(message)

@ExperimentalAnalysisCodegenApi
public class UnsupportedHybridConversionException(message: String) : IllegalArgumentException(message)

/**
 * ksp-bridge の KS 実装クラスが実装する内部用の口。[withAnalysis] はこれを探す。
 */
@InternalAnalysisCodegenApi
public interface HybridAnalysisAccess {
    public fun <R> withAnalysisOf(
        target: Any,
        block: KaSession.(Any) -> R,
    ): R
}

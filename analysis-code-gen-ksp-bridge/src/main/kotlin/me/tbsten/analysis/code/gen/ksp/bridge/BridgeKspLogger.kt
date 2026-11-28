@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.ksp.bridge

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.NonExistLocation
import me.tbsten.analysis.code.gen.engine.spi.EngineLogger
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi

/**
 * KSP の [KSPLogger] をエンジンのログへ流す（ADR-017、ADR-062）。
 *
 * KSNode の位置は KSP2 のコマンドラインのロガーと同じ `<パス>:<行>: <メッセージ>` の形でメッセージに入れる（BRG-06）。
 * error はエンジンがビルドの失敗として扱う。
 */
internal class BridgeKspLogger(
    private val delegate: EngineLogger,
) : KSPLogger {
    override fun logging(
        message: String,
        symbol: KSNode?,
    ) = log(EngineLogger.Severity.Logging, message, symbol)

    override fun info(
        message: String,
        symbol: KSNode?,
    ) = log(EngineLogger.Severity.Info, message, symbol)

    override fun warn(
        message: String,
        symbol: KSNode?,
    ) = log(EngineLogger.Severity.Warn, message, symbol)

    override fun error(
        message: String,
        symbol: KSNode?,
    ) = log(EngineLogger.Severity.Error, message, symbol)

    override fun exception(e: Throwable) = log(EngineLogger.Severity.Error, e.stackTraceToString(), null)

    private fun log(
        severity: EngineLogger.Severity,
        message: String,
        symbol: KSNode?,
    ) {
        val location =
            when (val l = symbol?.location) {
                is FileLocation -> "${l.filePath}:${l.lineNumber}: "
                is NonExistLocation, null -> ""
            }
        delegate.log(severity, location + message)
    }
}

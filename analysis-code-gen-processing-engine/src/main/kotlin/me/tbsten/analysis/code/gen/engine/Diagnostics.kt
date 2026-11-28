@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.engine

import com.intellij.psi.PsiElement
import me.tbsten.analysis.code.gen.engine.spi.EngineLogger
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenApiVersion
import me.tbsten.analysis.code.gen.processor.CodegenLogger
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import org.jetbrains.kotlin.psi.KtElement
import java.io.File

/** 1 実行単位の診断を集める。error が 1 件でもあれば、その実行単位は失敗（ADR-013）。 */
internal class DiagnosticCollector(
    private val rootDir: File,
    private val frameworkVersion: String,
) {
    private val list = mutableListOf<Diagnostic>()

    val diagnostics: List<Diagnostic> get() = synchronized(list) { list.toList() }

    val hasErrors: Boolean get() = synchronized(list) { list.any { it.severity == Diagnostic.Severity.Error } }

    fun report(
        severity: Diagnostic.Severity,
        message: String,
        processorName: String?,
        element: PsiElement? = null,
    ) {
        val (file, line) = element?.let(::locate) ?: (null to null)
        synchronized(list) { list += Diagnostic(severity, message, processorName, file, line) }
    }

    /** 例外を、段階・スタックトレース付きの error にする（API-11）。LinkageError なら版の非互換を疑う一文を足す（VER-02）。 */
    fun exception(
        processorName: String?,
        phase: String,
        throwable: Throwable,
    ) {
        val cause = if (throwable is ProcessorFailure) throwable.cause ?: throwable else throwable
        val hint =
            if (cause is LinkageError) {
                "\nprocessor-api の非互換の可能性があります（analysisCodeGen $frameworkVersion、" +
                    "processor-api の互換性番号 ${AnalysisCodegenApiVersion.CURRENT}）。Processor が想定する版を確かめてください。"
            } else {
                ""
            }
        report(Diagnostic.Severity.Error, "$phase で例外が出た: ${cause.stackTraceToString().trimEnd()}$hint", processorName)
    }

    private fun locate(element: PsiElement): Pair<String?, Int?> =
        runCatching {
            val file = element.containingFile
            val path = file?.virtualFile?.path?.let(::relativize) ?: file?.name
            val text = file?.text
            val line = text?.let { t -> t.substring(0, element.textOffset.coerceIn(0, t.length)).count { it == '\n' } + 1 }
            path to line
        }.getOrDefault(null to null)

    private fun relativize(path: String): String {
        val f = File(path)
        return if (f.isAbsolute && f.startsWith(rootDir)) f.relativeTo(rootDir).invariantSeparatorsPath else path
    }
}

/** Native の logger。`logging` は info と同じ扱い（エンジンの診断に debug の段階が無いため）。 */
internal class CodegenLoggerImpl(
    private val collector: DiagnosticCollector,
    private val processorName: String,
) : CodegenLogger {
    override fun logging(
        message: String,
        element: PsiElement?,
    ) = collector.report(Diagnostic.Severity.Info, message, processorName, element)

    override fun info(
        message: String,
        element: PsiElement?,
    ) = collector.report(Diagnostic.Severity.Info, message, processorName, element)

    override fun warn(
        message: String,
        element: PsiElement?,
    ) = collector.report(Diagnostic.Severity.Warning, message, processorName, element)

    override fun error(
        message: String,
        element: PsiElement?,
    ) = collector.report(Diagnostic.Severity.Error, message, processorName, element)

    override fun exception(throwable: Throwable) = collector.exception(processorName, "Processor", throwable)
}

internal class EngineLoggerImpl(
    private val collector: DiagnosticCollector,
    private val processorName: String,
) : EngineLogger {
    override fun log(
        severity: EngineLogger.Severity,
        message: String,
        element: KtElement?,
    ) {
        val s =
            when (severity) {
                EngineLogger.Severity.Logging, EngineLogger.Severity.Info -> Diagnostic.Severity.Info
                EngineLogger.Severity.Warn -> Diagnostic.Severity.Warning
                EngineLogger.Severity.Error -> Diagnostic.Severity.Error
            }
        collector.report(s, message, processorName, element)
    }
}

/** 外から渡された環境（エンジン以外が作ったもの）のとき用。 */
internal class EngineLoggerAsCodegenLogger(
    private val logger: EngineLogger,
) : CodegenLogger {
    override fun logging(
        message: String,
        element: PsiElement?,
    ) = logger.log(EngineLogger.Severity.Logging, message, element as? KtElement)

    override fun info(
        message: String,
        element: PsiElement?,
    ) = logger.log(EngineLogger.Severity.Info, message, element as? KtElement)

    override fun warn(
        message: String,
        element: PsiElement?,
    ) = logger.log(EngineLogger.Severity.Warn, message, element as? KtElement)

    override fun error(
        message: String,
        element: PsiElement?,
    ) = logger.log(EngineLogger.Severity.Error, message, element as? KtElement)

    override fun exception(throwable: Throwable) = logger.log(EngineLogger.Severity.Error, throwable.stackTraceToString())
}

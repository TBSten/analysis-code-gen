// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// Copyright kotlin-compile-testing contributors (tschuchortdev/kotlin-compile-testing, ZacSweers/kotlin-compile-testing)
// Ported-From: ZacSweers/kotlin-compile-testing@244ec46c6e280e0a7e8ab7a65022a657bd6824e8:ksp/src/main/kotlin/com/tschuchort/compiletesting/TestKSPLogger.kt
// Modified-By: analysis-code-gen contributors
// Modifications: パッケージを me.tbsten.analysiscodegen.testing.kct へ移動、@AutoService を手書きの META-INF/services に置換、コンパイラプラグイン ID を変更（それ以外の処理は変えていない）
package me.tbsten.analysiscodegen.testing.kct

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.NonExistLocation
import java.io.PrintWriter
import java.io.StringWriter
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

internal class TestKSPLogger(
  private val messageCollector: MessageCollector,
  private val allWarningsAsErrors: Boolean,
) : KSPLogger {

  companion object {
    const val PREFIX = "[ksp] "
  }

  data class Event(val severity: CompilerMessageSeverity, val message: String)

  val recordedEvents = mutableListOf<Event>()

  private val reportToCompilerSeverity =
    setOf(CompilerMessageSeverity.ERROR, CompilerMessageSeverity.EXCEPTION)

  private var reportedToCompiler = false

  private fun convertMessage(message: String, symbol: KSNode?): String =
    when (val location = symbol?.location) {
      is FileLocation -> "$PREFIX${location.filePath}:${location.lineNumber}: $message"
      is NonExistLocation,
      null -> "$PREFIX$message"
    }

  override fun logging(message: String, symbol: KSNode?) {
    recordedEvents.add(Event(CompilerMessageSeverity.LOGGING, convertMessage(message, symbol)))
  }

  override fun info(message: String, symbol: KSNode?) {
    recordedEvents.add(Event(CompilerMessageSeverity.INFO, convertMessage(message, symbol)))
  }

  override fun warn(message: String, symbol: KSNode?) {
    val severity =
      if (allWarningsAsErrors) CompilerMessageSeverity.ERROR else CompilerMessageSeverity.WARNING
    recordedEvents.add(Event(severity, convertMessage(message, symbol)))
  }

  override fun error(message: String, symbol: KSNode?) {
    recordedEvents.add(Event(CompilerMessageSeverity.ERROR, convertMessage(message, symbol)))
  }

  override fun exception(e: Throwable) {
    val writer = StringWriter()
    e.printStackTrace(PrintWriter(writer))
    recordedEvents.add(Event(CompilerMessageSeverity.EXCEPTION, writer.toString()))
  }

  fun reportAll() {
    for (event in recordedEvents) {
      if (!reportedToCompiler && event.severity in reportToCompilerSeverity) {
        reportedToCompiler = true
        messageCollector.report(event.severity, "Error occurred in KSP, check log for detail")
      }
      messageCollector.report(event.severity, event.message)
    }
  }
}

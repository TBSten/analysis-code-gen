// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// Copyright kotlin-compile-testing contributors (tschuchortdev/kotlin-compile-testing, ZacSweers/kotlin-compile-testing)
// Ported-From: ZacSweers/kotlin-compile-testing@244ec46c6e280e0a7e8ab7a65022a657bd6824e8:core/src/main/kotlin/com/tschuchort/compiletesting/MutliMessageCollector.kt
// Modified-By: analysis-code-gen contributors
// Modifications: パッケージを me.tbsten.analysis.code.gen.testing.kct へ移動、@AutoService を手書きの META-INF/services に置換、コンパイラプラグイン ID を変更（それ以外の処理は変えていない）
package me.tbsten.analysis.code.gen.testing.kct

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

internal class MultiMessageCollector(private vararg val collectors: MessageCollector) :
  MessageCollector {

  override fun clear() {
    collectors.forEach { it.clear() }
  }

  override fun hasErrors(): Boolean {
    return collectors.any { it.hasErrors() }
  }

  override fun report(
    severity: CompilerMessageSeverity,
    message: String,
    location: CompilerMessageSourceLocation?,
  ) {
    collectors.forEach { it.report(severity, message, location) }
  }
}

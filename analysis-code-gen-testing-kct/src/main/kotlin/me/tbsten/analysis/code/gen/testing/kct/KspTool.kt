// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// Copyright kotlin-compile-testing contributors (tschuchortdev/kotlin-compile-testing, ZacSweers/kotlin-compile-testing)
// Ported-From: ZacSweers/kotlin-compile-testing@244ec46c6e280e0a7e8ab7a65022a657bd6824e8:ksp/src/main/kotlin/com/tschuchort/compiletesting/KspTool.kt
// Modified-By: analysis-code-gen contributors
// Modifications: パッケージを me.tbsten.analysis.code.gen.testing.kct へ移動、@AutoService を手書きの META-INF/services に置換、コンパイラプラグイン ID を変更（それ以外の処理は変えていない）
package me.tbsten.analysis.code.gen.testing.kct

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

sealed interface KspTool {
  val symbolProcessorProviders: MutableList<SymbolProcessorProvider>
  val processorOptions: MutableMap<String, String>
  var incremental: Boolean
  var incrementalLog: Boolean
  var allWarningsAsErrors: Boolean
  var withCompilation: Boolean
  var loggingLevels: Set<CompilerMessageSeverity>
}

/** Gets or creates the [KspTool] if it doesn't exist. */
@OptIn(ExperimentalCompilerApi::class)
internal fun KotlinCompilation.getKspTool(): KspTool {
  val ksp2Tool = precursorTools["ksp2"] as? Ksp2PrecursorTool?
  return ksp2Tool ?: error("KSP not configured")
}

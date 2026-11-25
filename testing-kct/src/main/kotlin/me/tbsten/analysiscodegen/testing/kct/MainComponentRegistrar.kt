// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// Copyright kotlin-compile-testing contributors (tschuchortdev/kotlin-compile-testing, ZacSweers/kotlin-compile-testing)
// Ported-From: ZacSweers/kotlin-compile-testing@244ec46c6e280e0a7e8ab7a65022a657bd6824e8:core/src/main/kotlin/com/tschuchort/compiletesting/MainComponentRegistrar.kt
// Modified-By: analysis-code-gen contributors
// Modifications: パッケージを me.tbsten.analysiscodegen.testing.kct へ移動、@AutoService を手書きの META-INF/services に置換、コンパイラプラグイン ID を変更（それ以外の処理は変えていない）
/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("DEPRECATION")

package me.tbsten.analysiscodegen.testing.kct

import me.tbsten.analysiscodegen.testing.kct.kapt.KctKaptCompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.kapt.base.KaptOptions
import org.jetbrains.kotlin.kapt.base.incremental.IncrementalProcessor

@ExperimentalCompilerApi
internal class MainComponentRegistrar : CompilerPluginRegistrar() {

  override val pluginId: String = "me.tbsten.analysiscodegen.testing.kct"

  override val supportsK2: Boolean = true

  // Handle unset parameters gracefully because this plugin may be accidentally called by other
  // tools that
  // discover it on the classpath (for example the kotlin jupyter kernel).
  private fun getThreadLocalParameters(caller: String): ThreadLocalParameters? {
    val params = threadLocalParameters.get()
    if (params == null) {
      System.err.println(
        "WARNING: ${MainComponentRegistrar::class.simpleName}::$caller accessed before thread local parameters have been set"
      )
    }

    return params
  }

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val parameters = getThreadLocalParameters("registerExtensions") ?: return

    parameters.compilerPluginRegistrar.forEach { pluginRegistrar ->
      with(pluginRegistrar) { registerExtensions(configuration) }
    }

    with(KctKaptCompilerPluginRegistrar(parameters.processors, parameters.kaptOptions)) {
      registerExtensions(configuration)
    }
  }

  companion object {
    /*
     * This compiler plugin is instantiated by K2JVMCompiler using
     * a service locator. So we can't just pass parameters to it easily.
     * Instead, we need to use a thread-local global variable to pass
     * any parameters that change between compilations
     */
    val threadLocalParameters: ThreadLocal<ThreadLocalParameters> = ThreadLocal()
  }

  data class ThreadLocalParameters(
    val processors: List<IncrementalProcessor>,
    val kaptOptions: KaptOptions.Builder,
    val compilerPluginRegistrar: List<CompilerPluginRegistrar>,
  )
}

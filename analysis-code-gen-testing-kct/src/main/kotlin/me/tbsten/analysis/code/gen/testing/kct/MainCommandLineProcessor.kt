// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// Copyright kotlin-compile-testing contributors (tschuchortdev/kotlin-compile-testing, ZacSweers/kotlin-compile-testing)
// Ported-From: ZacSweers/kotlin-compile-testing@244ec46c6e280e0a7e8ab7a65022a657bd6824e8:core/src/main/kotlin/com/tschuchort/compiletesting/MainCommandLineProcessor.kt
// Modified-By: analysis-code-gen contributors
// Modifications: パッケージを me.tbsten.analysis.code.gen.testing.kct へ移動、@AutoService を手書きの META-INF/services に置換、コンパイラプラグイン ID を変更（それ以外の処理は変えていない）
package me.tbsten.analysis.code.gen.testing.kct

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@ExperimentalCompilerApi
internal class MainCommandLineProcessor : CommandLineProcessor {
  override val pluginId: String = Companion.pluginId

  override val pluginOptions: Collection<AbstractCliOption>
    get() =
      threadLocalParameters.get()?.pluginOptions
        ?: emptyList<AbstractCliOption>().also {
          // Handle unset parameters gracefully because this plugin may be accidentally called by
          // other tools that
          // discover it on the classpath (for example the kotlin jupyter kernel).
          System.err.println(
            "WARNING: MainCommandLineProcessor::pluginOptions accessed before thread local parameters have been set"
          )
        }

  companion object {
    const val pluginId = "me.tbsten.analysis.code.gen.testing.kct.maincommandlineprocessor"

    /**
     * This CommandLineProcessor is instantiated by K2JVMCompiler using a service locator. So we
     * can't just pass parameters to it easily. Instead, we need to use a thread-local global
     * variable to pass any parameters that change between compilations
     */
    val threadLocalParameters: ThreadLocal<ThreadLocalParameters> = ThreadLocal()

    private fun encode(str: String) =
      str // Base64.getEncoder().encodeToString(str.toByteArray()).replace('=', '%')

    private fun decode(str: String) =
      str // String(Base64.getDecoder().decode(str.replace('%', '=')))

    fun encodeForeignOptionName(processorPluginId: PluginId, optionName: OptionName) =
      encode(processorPluginId) + ":" + encode(optionName)

    fun decodeForeignOptionName(str: String): Pair<PluginId, OptionName> {
      return Regex("(.*?):(.*)").matchEntire(str)?.groupValues?.let {
        (_, encodedPluginId, encodedOptionName) ->
        Pair(decode(encodedPluginId), decode(encodedOptionName))
      } ?: error("Could not decode foreign option name: '$str'.")
    }
  }

  class ThreadLocalParameters(cliProcessors: List<CommandLineProcessor>) {
    val cliProcessorsByPluginId: Map<PluginId, List<CommandLineProcessor>> =
      cliProcessors.groupBy(CommandLineProcessor::pluginId)

    val optionByProcessorAndName: Map<Pair<CommandLineProcessor, OptionName>, AbstractCliOption> =
      cliProcessors
        .flatMap { cliProcessor ->
          cliProcessor.pluginOptions.map { option ->
            Pair(Pair(cliProcessor, option.optionName), option)
          }
        }
        .toMap()

    val pluginOptions = cliProcessorsByPluginId.flatMap { (processorPluginId, cliProcessors) ->
      cliProcessors.flatMap { cliProcessor ->
        cliProcessor.pluginOptions.map { option ->
          CliOption(
            encodeForeignOptionName(processorPluginId, option.optionName),
            option.valueDescription,
            option.description,
            option.required,
            option.allowMultipleOccurrences,
          )
        }
      }
    }
  }

  override fun processOption(
    option: AbstractCliOption,
    value: String,
    configuration: CompilerConfiguration,
  ) {
    // Handle unset parameters gracefully because this plugin may be accidentally called by other
    // tools that
    // discover it on the classpath (for example the kotlin jupyter kernel).
    if (threadLocalParameters.get() == null) {
      System.err.println(
        "WARNING: MainCommandLineProcessor::processOption accessed before thread local parameters have been set"
      )
      return
    }

    val (foreignPluginId, foreignOptionName) = decodeForeignOptionName(option.optionName)
    val params = threadLocalParameters.get()

    params.cliProcessorsByPluginId[foreignPluginId]?.forEach { cliProcessor ->
      cliProcessor.processOption(
        params.optionByProcessorAndName[Pair(cliProcessor, foreignOptionName)]
          ?: error("Could not get AbstractCliOption for option '$foreignOptionName'"),
        value,
        configuration,
      )
    }
      ?: error(
        "No CommandLineProcessor given for option '$foreignOptionName' for plugin ID $foreignPluginId"
      )
  }
}

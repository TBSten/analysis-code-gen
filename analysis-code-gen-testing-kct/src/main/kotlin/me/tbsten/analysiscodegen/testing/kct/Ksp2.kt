// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// Copyright kotlin-compile-testing contributors (tschuchortdev/kotlin-compile-testing, ZacSweers/kotlin-compile-testing)
// Ported-From: ZacSweers/kotlin-compile-testing@244ec46c6e280e0a7e8ab7a65022a657bd6824e8:ksp/src/main/kotlin/com/tschuchort/compiletesting/Ksp2.kt
// Modified-By: analysis-code-gen contributors
// Modifications: パッケージを me.tbsten.analysiscodegen.testing.kct へ移動、@AutoService を手書きの META-INF/services に置換、コンパイラプラグイン ID を変更（それ以外の処理は変えていない）
/* Adds support for KSP (https://goo.gle/ksp). */
package me.tbsten.analysiscodegen.testing.kct

import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import java.io.File
import java.io.PrintStream
import java.util.EnumSet
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@ExperimentalCompilerApi
class Ksp2PrecursorTool : PrecursorTool, KspTool {
  override var withCompilation: Boolean
    get() = false
    set(value) {
      // Irrelevant/unavailable on KSP 2
    }

  override val symbolProcessorProviders: MutableList<SymbolProcessorProvider> = mutableListOf()
  override val processorOptions: MutableMap<String, String> = mutableMapOf()
  override var incremental: Boolean = false
  override var incrementalLog: Boolean = false
  override var allWarningsAsErrors: Boolean = false
  override var loggingLevels: Set<CompilerMessageSeverity> =
    EnumSet.allOf(CompilerMessageSeverity::class.java)

  // Extra hook for direct configuration of KspJvmConfig.Builder, for advanced use cases
  var onBuilder: (KSPJvmConfig.Builder.() -> Unit)? = null

  override fun execute(
    compilation: KotlinCompilation,
    output: PrintStream,
    sources: List<File>,
  ): KotlinCompilation.ExitCode {
    if (symbolProcessorProviders.isEmpty()) {
      return KotlinCompilation.ExitCode.OK
    }

    val config =
      KSPJvmConfig.Builder()
        .apply {
          projectBaseDir = compilation.kspWorkingDir.absoluteFile

          incremental = this@Ksp2PrecursorTool.incremental
          incrementalLog = this@Ksp2PrecursorTool.incrementalLog
          allWarningsAsErrors = this@Ksp2PrecursorTool.allWarningsAsErrors
          processorOptions = this@Ksp2PrecursorTool.processorOptions.toMap()

          jvmTarget = compilation.jvmTarget
          jdkHome = compilation.jdkHome
          languageVersion = compilation.languageVersion ?: KotlinVersion.CURRENT.languageVersion()
          apiVersion = compilation.apiVersion ?: KotlinVersion.CURRENT.languageVersion()

          // TODO adopt new roots model
          moduleName = compilation.moduleName ?: "main"
          sourceRoots =
            sources
              .filter { it.extension == "kt" }
              .mapNotNull { it.parentFile.absoluteFile }
              .distinct()
          javaSourceRoots =
            sources
              .filter { it.extension == "java" }
              .mapNotNull { it.parentFile.absoluteFile }
              .distinct()
          libraries = compilation.classpaths + compilation.commonClasspaths()

          cachesDir =
            compilation.kspCachesDir
              .also {
                it.deleteRecursively()
                it.mkdirs()
              }
              .absoluteFile
          outputBaseDir =
            compilation.kspSourcesDir
              .also {
                it.deleteRecursively()
                it.mkdirs()
              }
              .absoluteFile
          classOutputDir =
            compilation.kspClassesDir
              .also {
                it.deleteRecursively()
                it.mkdirs()
              }
              .absoluteFile
          javaOutputDir =
            compilation.kspJavaSourceDir
              .also {
                it.deleteRecursively()
                it.mkdirs()
                compilation.registerGeneratedSourcesDir(it)
              }
              .absoluteFile
          kotlinOutputDir =
            compilation.kspKotlinSourceDir
              .also {
                it.deleteRecursively()
                it.mkdirs()
                compilation.registerGeneratedSourcesDir(it)
              }
              .absoluteFile
          resourceOutputDir =
            compilation.kspResources
              .also {
                it.deleteRecursively()
                it.mkdirs()
              }
              .absoluteFile

          onBuilder?.invoke(this)
        }
        .build()

    val messageCollector = compilation.createMessageCollectorAccess("ksp")
    val logger =
      TestKSPLogger(
        messageCollector = messageCollector,
        allWarningsAsErrors = config.allWarningsAsErrors,
      )

    return try {
      when (KotlinSymbolProcessing(config, symbolProcessorProviders.toList(), logger).execute()) {
        KotlinSymbolProcessing.ExitCode.OK -> KotlinCompilation.ExitCode.OK
        KotlinSymbolProcessing.ExitCode.PROCESSING_ERROR ->
          KotlinCompilation.ExitCode.COMPILATION_ERROR
      }
    } finally {
      logger.reportAll()
    }
  }
}

private fun KotlinVersion.languageVersion(): String {
  return "$major.$minor"
}

/** Enables KSP2. */
@OptIn(ExperimentalCompilerApi::class)
fun KotlinCompilation.useKsp2() {
  precursorTools.getOrPut("ksp2", ::Ksp2PrecursorTool)
}

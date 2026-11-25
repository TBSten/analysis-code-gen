// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// Copyright kotlin-compile-testing contributors (tschuchortdev/kotlin-compile-testing, ZacSweers/kotlin-compile-testing)
// Ported-From: ZacSweers/kotlin-compile-testing@244ec46c6e280e0a7e8ab7a65022a657bd6824e8:core/src/main/kotlin/com/tschuchort/compiletesting/CompilationResult.kt
// Modified-By: analysis-code-gen contributors
// Modifications: パッケージを me.tbsten.analysiscodegen.testing.kct へ移動、@AutoService を手書きの META-INF/services に置換、コンパイラプラグイン ID を変更、kapt（Java の注釈処理）への対応を削除（porting/kct/patches/0001-remove-kapt.patch）
package me.tbsten.analysiscodegen.testing.kct

import java.io.File
import java.net.URLClassLoader
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

/** Result of the compilation. */
@ExperimentalCompilerApi
sealed interface CompilationResult {
  /** The exit code of the compilation. */
  val exitCode: KotlinCompilation.ExitCode
  /** Messages that were printed by the compilation. */
  val messages: String
  /** Messages with captured diagnostic severity. */
  val diagnosticMessages: List<DiagnosticMessage>
  /** The directory where compiled files will be output to. */
  val outputDirectory: File

  /** Messages filtered by the given severities */
  fun messagesWithSeverity(vararg severities: DiagnosticSeverity): String =
    diagnosticMessages.filter { it.severity in severities }.joinToString("\n")
}

@ExperimentalCompilerApi
class JsCompilationResult(
  override val exitCode: KotlinCompilation.ExitCode,
  override val messages: String,
  override val diagnosticMessages: List<DiagnosticMessage>,
  private val compilation: KotlinJsCompilation,
) : CompilationResult {
  override val outputDirectory: File
    get() = compilation.outputDir

  /** JS files output by the compilation. */
  val jsFiles: List<File> = outputDirectory.listFilesRecursively()
}

/** Result of the compilation */
@ExperimentalCompilerApi
class JvmCompilationResult(
  override val exitCode: KotlinCompilation.ExitCode,
  override val messages: String,
  override val diagnosticMessages: List<DiagnosticMessage>,
  private val compilation: KotlinCompilation,
) : CompilationResult {
  override val outputDirectory: File
    get() = compilation.classesDir

  /** ClassLoader to load the compiled classes */
  val classLoader =
    URLClassLoader(
      // Include the original classpaths and the output directory to be able to load classes from
      // dependencies.
      compilation.classpaths.plus(outputDirectory).map { it.toURI().toURL() }.toTypedArray(),
      this::class.java.classLoader,
    )

  /** Compiled classes and resources output by the compilation. */
  val compiledClassAndResourceFiles: List<File> = outputDirectory.listFilesRecursively()

  /** The class and resource files generated during the compilation. */
  val generatedFiles: Collection<File> = compiledClassAndResourceFiles
}

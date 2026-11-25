// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// Copyright kotlin-compile-testing contributors (tschuchortdev/kotlin-compile-testing, ZacSweers/kotlin-compile-testing)
// Ported-From: ZacSweers/kotlin-compile-testing@244ec46c6e280e0a7e8ab7a65022a657bd6824e8:core/src/main/kotlin/com/tschuchort/compiletesting/KotlinJsCompilation.kt
// Modified-By: analysis-code-gen contributors
// Modifications: パッケージを me.tbsten.analysis.code.gen.testing.kct へ移動、@AutoService を手書きの META-INF/services に置換、コンパイラプラグイン ID を変更（それ以外の処理は変えていない）
package me.tbsten.analysis.code.gen.testing.kct

import java.io.File
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@ExperimentalCompilerApi
@Suppress("MemberVisibilityCanBePrivate")
class KotlinJsCompilation : AbstractKotlinCompilation<K2JSCompilerArguments>() {

  @Deprecated("It is senseless to use with IR compiler. Only for compatibility.")
  var outputFileName: String? = null

  /**
   * Generate unpacked KLIB into parent directory of output JS file. In combination with -meta-info
   * generates both IR and pre-IR versions of library.
   */
  var irProduceKlibDir: Boolean = false

  /** Generate packed klib into file specified by -output. Disables pre-IR backend */
  var irProduceKlibFile: Boolean = false

  /** Generates JS file using IR backend. Also disables pre-IR backend */
  var irProduceJs: Boolean = true

  /** Perform experimental dead code elimination */
  var irDce: Boolean = false

  /** Print declarations' reachability info to stdout during performing DCE */
  var irDcePrintReachabilityInfo: Boolean = false

  /** Specify a compilation module name for IR backend */
  var irModuleName: String? = null

  /** Base name of generated files */
  var moduleName: String? = null

  /**
   * Path to the kotlin-stdlib-js.jar If none is given, it will be searched for in the host process'
   * classpaths
   */
  var kotlinStdLibJsJar: File? by default { HostEnvironment.kotlinStdLibJsJar }

  /** Generate TypeScript declarations .d.ts file alongside JS file. Available in IR backend only */
  var generateDts: Boolean = false

  // *.class files, Jars and resources (non-temporary) that are created by the
  // compilation will land here
  val outputDir
    get() = workingDir.resolve("output")

  // setup common arguments for the two kotlinc calls
  private fun jsArgs() =
    commonArguments(K2JSCompilerArguments()) { args ->
      args.moduleKind = "commonjs"
      args.outputDir = outputDir.absolutePath
      args.sourceMapBaseDirs = jsClasspath().joinToString(separator = File.pathSeparator)
      args.libraries = listOfNotNull(kotlinStdLibJsJar).joinToString(separator = ":")

      args.irProduceKlibDir = irProduceKlibDir
      args.irProduceKlibFile = irProduceKlibFile
      args.irProduceJs = irProduceJs
      args.irDce = irDce
      args.irDcePrintReachabilityInfo = irDcePrintReachabilityInfo
      args.moduleName = moduleName
      args.irModuleName = irModuleName
      args.generateDts = generateDts
    }

  /** Runs the compilation task */
  fun compile(): JsCompilationResult {
    // make sure all needed directories exist
    sourcesDir.deleteRecursively()
    sourcesDir.mkdirs()
    outputDir.mkdirs()

    // write given sources to working directory
    val sourceFiles = sources.map { it.writeTo(sourcesDir) }

    pluginClasspaths.forEach { filepath ->
      if (!filepath.exists()) {
        error("Plugin $filepath not found")
        return makeResult(KotlinCompilation.ExitCode.INTERNAL_ERROR)
      }
    }

    /* Work around for warning that sometimes happens:
    "Failed to initialize native filesystem for Windows
    java.lang.RuntimeException: Could not find installation home path.
    Please make sure bin/idea.properties is present in the installation directory"
    See: https://github.com/arturbosch/detekt/issues/630
    */
    withSystemProperty("idea.use.native.fs.for.win", "false") {
      // step 1: compile Kotlin files
      return makeResult(compileKotlin(sourceFiles, K2JSCompiler(), jsArgs()))
    }
  }

  private fun makeResult(exitCode: KotlinCompilation.ExitCode): JsCompilationResult {
    val messages = internalMessageBuffer.readUtf8()

    if (exitCode != KotlinCompilation.ExitCode.OK) searchSystemOutForKnownErrors(messages)

    return JsCompilationResult(exitCode, messages, diagnostics, this)
  }

  private fun jsClasspath() =
    mutableListOf<File>()
      .apply {
        addAll(classpaths)
        addAll(listOfNotNull(kotlinStdLibCommonJar, kotlinStdLibJsJar))

        if (inheritClassPath) {
          addAll(hostClasspaths)
          log("Inheriting classpaths:  " + hostClasspaths.joinToString(File.pathSeparator))
        }
      }
      .distinct()
}

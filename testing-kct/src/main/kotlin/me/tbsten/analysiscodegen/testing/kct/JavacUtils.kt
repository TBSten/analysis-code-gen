// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// Copyright kotlin-compile-testing contributors (tschuchortdev/kotlin-compile-testing, ZacSweers/kotlin-compile-testing)
// Ported-From: ZacSweers/kotlin-compile-testing@244ec46c6e280e0a7e8ab7a65022a657bd6824e8:core/src/main/kotlin/com/tschuchort/compiletesting/JavacUtils.kt
// Modified-By: analysis-code-gen contributors
// Modifications: パッケージを me.tbsten.analysiscodegen.testing.kct へ移動、@AutoService を手書きの META-INF/services に置換、コンパイラプラグイン ID を変更（それ以外の処理は変えていない）
package me.tbsten.analysiscodegen.testing.kct

import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.io.StringReader
import java.net.URI
import java.nio.charset.Charset
import javax.tools.JavaCompiler
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import okio.Buffer

/**
 * A [JavaFileObject] created from a source [File].
 *
 * Used for interfacing with javac ([JavaCompiler]).
 */
internal class FileJavaFileObject(
  val sourceFile: File,
  val charset: Charset = Charset.defaultCharset(),
) : SimpleJavaFileObject(sourceFile.toURI(), deduceKind(sourceFile.toURI())) {

  init {
    require(sourceFile.isFile)
    require(sourceFile.canRead())
  }

  companion object {
    private fun deduceKind(uri: URI): JavaFileObject.Kind =
      JavaFileObject.Kind.values().firstOrNull {
        uri.path.endsWith(it.extension, ignoreCase = true)
      } ?: JavaFileObject.Kind.OTHER
  }

  override fun openInputStream(): InputStream = sourceFile.inputStream()

  override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence =
    sourceFile.readText(charset)
}

/**
 * A [JavaFileObject] created from a [String].
 *
 * Used for interfacing with javac ([JavaCompiler]).
 */
internal class StringJavaFileObject(className: String, private val contents: String) :
  SimpleJavaFileObject(
    URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension),
    JavaFileObject.Kind.SOURCE,
  ) {
  private var _lastModified = System.currentTimeMillis()

  override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = contents

  override fun openInputStream(): InputStream =
    ByteArrayInputStream(contents.toByteArray(Charset.defaultCharset()))

  override fun openReader(ignoreEncodingErrors: Boolean): Reader = StringReader(contents)

  override fun getLastModified(): Long = _lastModified
}

/**
 * Gets the version string of a javac executable that can be started using the given [javacCommand]
 * via [Runtime.exec].
 */
internal fun getJavacVersionString(javacCommand: String): String {
  val javacProc = ProcessBuilder(listOf(javacCommand, "-version")).redirectErrorStream(true).start()

  val buffer = Buffer()

  javacProc.inputStream.copyTo(buffer.outputStream())
  javacProc.waitFor()

  val output = buffer.readUtf8()

  return parseVersionString(output)
    ?: throw IllegalStateException(
      "Command '$javacCommand -version' did not print expected output. " + "Output was: '$output'"
    )
}

internal fun parseVersionString(output: String) =
  Regex("javac (.*)?[\\s\\S]*").find(output)?.destructured?.component1()

internal fun isJavac9OrLater(javacVersionString: String): Boolean {
  try {
    val (majorv, minorv, patchv, otherv) =
      Regex("([0-9]*)(?:\\.([0-9]*))?(?:\\.([0-9]*))?(.*)")
        .matchEntire(javacVersionString)
        ?.destructured ?: throw IllegalArgumentException("Could not match version regex")

    check(majorv.isNotBlank()) { "Major version can not be blank" }

    if (majorv.toInt() == 1)
      check(minorv.isNotBlank()) { "Minor version can not be blank if major version is 1" }

    return (majorv.toInt() == 1 && minorv.toInt() >= 9) // old versioning scheme: 1.8.x
    || (majorv.toInt() >= 9) // new versioning scheme: 10.x.x
  } catch (t: Throwable) {
    throw IllegalArgumentException("Could not parse javac version string: '$javacVersionString'", t)
  }
}

/** Finds the tools.jar given a path to a JDK 8 or earlier */
internal fun findToolsJarFromJdk(jdkHome: File): File {
  return jdkHome.resolve("../lib/tools.jar").existsOrNull()
    ?: jdkHome.resolve("lib/tools.jar").existsOrNull()
    ?: jdkHome.resolve("tools.jar").existsOrNull()
    ?: throw IllegalStateException(
      "Can not find tools.jar from JDK with path ${jdkHome.absolutePath}"
    )
}

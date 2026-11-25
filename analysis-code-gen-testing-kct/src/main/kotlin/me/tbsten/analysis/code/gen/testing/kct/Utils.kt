// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// Copyright kotlin-compile-testing contributors (tschuchortdev/kotlin-compile-testing, ZacSweers/kotlin-compile-testing)
// Ported-From: ZacSweers/kotlin-compile-testing@244ec46c6e280e0a7e8ab7a65022a657bd6824e8:core/src/main/kotlin/com/tschuchort/compiletesting/Utils.kt
// Modified-By: analysis-code-gen contributors
// Modifications: パッケージを me.tbsten.analysis.code.gen.testing.kct へ移動、@AutoService を手書きの META-INF/services に置換、コンパイラプラグイン ID を変更（それ以外の処理は変えていない）
package me.tbsten.analysis.code.gen.testing.kct

import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import java.net.URL
import java.net.URLClassLoader
import java.nio.charset.Charset
import javax.lang.model.SourceVersion
import okio.Buffer

internal fun <E> MutableCollection<E>.addAll(vararg elems: E) = addAll(elems)

internal fun getJavaHome(): File {
  val path =
    System.getProperty("java.home")
      ?: System.getenv("JAVA_HOME")
      ?: throw IllegalStateException("no java home found")

  return File(path).also { check(it.isDirectory) }
}

internal val processJdkHome by lazy {
  if (isJdk9OrLater()) getJavaHome() else getJavaHome().parentFile
}

/** Checks if the JDK of the host process is version 9 or later */
internal fun isJdk9OrLater(): Boolean =
  SourceVersion.latestSupported().compareTo(SourceVersion.RELEASE_8) > 0

internal fun File.listFilesRecursively(): List<File> = walkTopDown().filter { it.isFile }.toList()

internal fun File.hasKotlinFileExtension() = hasFileExtension(listOf("kt", "kts"))

internal fun File.hasJavaFileExtension() = hasFileExtension(listOf("java"))

internal fun File.hasSourceFileExtension() = hasFileExtension(listOf("kt", "java"))

internal fun File.hasFileExtension(extensions: List<String>) = extensions.any {
  it.equals(extension, ignoreCase = true)
}

internal fun URLClassLoader.addUrl(url: URL) {
  val addUrlMethod = URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java)
  addUrlMethod.isAccessible = true
  addUrlMethod.invoke(this, url)
}

internal inline fun <T> withSystemProperty(key: String, value: String, f: () -> T): T =
  withSystemProperties(mapOf(key to value), f)

internal inline fun <T> withSystemProperties(properties: Map<String, String>, f: () -> T): T {
  val previousProperties = mutableMapOf<String, String?>()

  for ((key, value) in properties) {
    previousProperties[key] = System.getProperty(key)
    System.setProperty(key, value)
  }

  try {
    return f()
  } finally {
    for ((key, value) in previousProperties) {
      if (value != null) System.setProperty(key, value)
    }
  }
}

internal inline fun <R> withSystemOut(stream: PrintStream, crossinline f: () -> R): R {
  System.setOut(stream)
  val ret = f()
  System.setOut(PrintStream(FileOutputStream(FileDescriptor.out)))
  return ret
}

internal inline fun <R> captureSystemOut(crossinline f: () -> R): Pair<R, String> {
  val systemOutBuffer = Buffer()
  val ret = withSystemOut(PrintStream(systemOutBuffer.outputStream()), f)
  return Pair(ret, systemOutBuffer.readString(Charset.defaultCharset()))
}

internal fun File.existsOrNull(): File? = if (exists()) this else null

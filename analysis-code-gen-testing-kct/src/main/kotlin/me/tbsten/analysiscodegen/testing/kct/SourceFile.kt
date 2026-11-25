// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// Copyright kotlin-compile-testing contributors (tschuchortdev/kotlin-compile-testing, ZacSweers/kotlin-compile-testing)
// Ported-From: ZacSweers/kotlin-compile-testing@244ec46c6e280e0a7e8ab7a65022a657bd6824e8:core/src/main/kotlin/com/tschuchort/compiletesting/SourceFile.kt
// Modified-By: analysis-code-gen contributors
// Modifications: パッケージを me.tbsten.analysiscodegen.testing.kct へ移動、@AutoService を手書きの META-INF/services に置換、コンパイラプラグイン ID を変更（それ以外の処理は変えていない）
package me.tbsten.analysiscodegen.testing.kct

import java.io.File
import okio.buffer
import okio.sink
import org.intellij.lang.annotations.Language

/** A source file for the [KotlinCompilation] */
abstract class SourceFile {
  internal abstract fun writeTo(dir: File): File

  companion object {
    /** Create a new Java source file for the compilation when the compilation is run */
    fun java(
      name: String,
      @Language("java") contents: String,
      trimIndent: Boolean = true,
    ): SourceFile {
      require(File(name).hasJavaFileExtension())
      val finalContents = if (trimIndent) contents.trimIndent() else contents
      return new(name, finalContents)
    }

    /** Create a new Kotlin source file for the compilation when the compilation is run */
    fun kotlin(
      name: String,
      @Language("kotlin") contents: String,
      trimIndent: Boolean = true,
    ): SourceFile {
      require(File(name).hasKotlinFileExtension())
      val finalContents = if (trimIndent) contents.trimIndent() else contents
      return new(name, finalContents)
    }

    /** Create a new source file for the compilation when the compilation is run */
    fun new(name: String, contents: String) =
      object : SourceFile() {
        override fun writeTo(dir: File): File {
          val file = dir.resolve(name)
          file.parentFile.mkdirs()
          file.createNewFile()

          file.sink().buffer().use { it.writeUtf8(contents) }

          return file
        }
      }

    /** Compile an existing source file */
    @Deprecated("This will not work reliably with KSP, use `new` instead")
    fun fromPath(path: File) =
      object : SourceFile() {
        init {
          require(path.isFile)
        }

        override fun writeTo(dir: File): File = path
      }
  }
}

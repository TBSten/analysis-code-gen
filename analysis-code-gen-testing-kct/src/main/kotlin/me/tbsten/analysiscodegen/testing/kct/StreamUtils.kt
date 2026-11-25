// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// Copyright kotlin-compile-testing contributors (tschuchortdev/kotlin-compile-testing, ZacSweers/kotlin-compile-testing)
// Ported-From: ZacSweers/kotlin-compile-testing@244ec46c6e280e0a7e8ab7a65022a657bd6824e8:core/src/main/kotlin/com/tschuchort/compiletesting/StreamUtils.kt
// Modified-By: analysis-code-gen contributors
// Modifications: パッケージを me.tbsten.analysiscodegen.testing.kct へ移動、@AutoService を手書きの META-INF/services に置換、コンパイラプラグイン ID を変更（それ以外の処理は変えていない）
package me.tbsten.analysiscodegen.testing.kct

import java.io.IOException
import java.io.OutputStream

/** An output stream that does nothing, like /dev/null */
internal object NullStream : OutputStream() {
  override fun write(b: Int) {
    // NoOp
  }

  override fun close() {
    // NoOp
  }

  override fun flush() {
    // NoOp
  }

  override fun write(b: ByteArray, off: Int, len: Int) {
    // NoOp
  }

  override fun write(b: ByteArray) {
    // NoOp
  }
}

/** A combined stream that writes to all the output streams in [streams]. */
@Suppress("MemberVisibilityCanBePrivate")
internal class TeeOutputStream(val streams: Collection<OutputStream>) : OutputStream() {

  constructor(vararg streams: OutputStream) : this(streams.toList())

  @Synchronized
  @Throws(IOException::class)
  override fun write(b: Int) {
    for (stream in streams) stream.write(b)
  }

  @Synchronized
  @Throws(IOException::class)
  override fun write(b: ByteArray) {
    for (stream in streams) stream.write(b)
  }

  @Synchronized
  @Throws(IOException::class)
  override fun write(b: ByteArray, off: Int, len: Int) {
    for (stream in streams) stream.write(b, off, len)
  }

  @Throws(IOException::class)
  override fun flush() {
    for (stream in streams) stream.flush()
  }

  @Throws(IOException::class)
  override fun close() {
    closeImpl(streams)
  }

  @Throws(IOException::class)
  private fun closeImpl(streamsToClose: Collection<OutputStream>) {
    try {
      streamsToClose.firstOrNull()?.close()
    } finally {
      if (streamsToClose.size > 1) closeImpl(streamsToClose.drop(1))
    }
  }
}

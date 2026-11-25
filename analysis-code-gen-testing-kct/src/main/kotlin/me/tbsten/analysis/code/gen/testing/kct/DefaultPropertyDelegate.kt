// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// Copyright kotlin-compile-testing contributors (tschuchortdev/kotlin-compile-testing, ZacSweers/kotlin-compile-testing)
// Ported-From: ZacSweers/kotlin-compile-testing@244ec46c6e280e0a7e8ab7a65022a657bd6824e8:core/src/main/kotlin/com/tschuchort/compiletesting/DefaultPropertyDelegate.kt
// Modified-By: analysis-code-gen contributors
// Modifications: パッケージを me.tbsten.analysis.code.gen.testing.kct へ移動、@AutoService を手書きの META-INF/services に置換、コンパイラプラグイン ID を変更（それ以外の処理は変えていない）
package me.tbsten.analysis.code.gen.testing.kct

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Suppress("MemberVisibilityCanBePrivate")
internal class DefaultPropertyDelegate<R, T>(private val createDefault: () -> T) :
  ReadWriteProperty<R, T> {
  val hasDefaultValue
    @Synchronized get() = (value == DEFAULT)

  private var value: Any? = DEFAULT
  val defaultValue by lazy { createDefault() }

  @Synchronized
  override operator fun getValue(thisRef: R, property: KProperty<*>): T {
    @Suppress("UNCHECKED_CAST")
    return if (hasDefaultValue) defaultValue else value as T
  }

  @Synchronized
  override operator fun setValue(thisRef: R, property: KProperty<*>, value: T) {
    this.value = value
  }

  companion object {
    private object DEFAULT
  }
}

internal fun <R, T> default(createDefault: () -> T) = DefaultPropertyDelegate<R, T>(createDefault)

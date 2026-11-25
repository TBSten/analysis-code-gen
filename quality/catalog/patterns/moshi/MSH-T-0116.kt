// PATTERN: MSH-T-0116
// TITLE: 実行時テスト / DualKotlinTest / InternalAbstractProperty
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=inheritance, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/DualKotlinTest.kt#InternalAbstractProperty
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: InternalAbstractProperty.kt
package com.squareup.moshi.kotlin

import com.squareup.moshi.JsonClass

abstract class InternalAbstractPropertyBase {
  internal abstract val test: String

  // Regression for https://github.com/square/moshi/issues/974
  abstract fun abstractFun(): String
}

@JsonClass(generateAdapter = true)
class InternalAbstractProperty(override val test: String) : InternalAbstractPropertyBase() {
  override fun abstractFun(): String {
    return test
  }
}

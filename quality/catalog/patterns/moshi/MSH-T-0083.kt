// PATTERN: MSH-T-0083
// TITLE: 実行時テスト / GeneratedAdaptersTest / PrivateTransient
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=inheritance, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#PrivateTransient
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: PrivateTransient.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PrivateTransient {
  @Transient private var a: Int = -1
  var b: Int = -1

  fun readA(): Int {
    return a
  }

  fun writeA(a: Int) {
    this.a = a
  }
}

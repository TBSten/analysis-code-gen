// PATTERN: MSH-T-0070
// TITLE: 実行時テスト / GeneratedAdaptersTest / TransientProperty
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#TransientProperty
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: TransientProperty.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class TransientProperty {
  @Transient var a: Int = -1
  @Transient private var b: Int = -1
  var c: Int = -1

  fun getB() = b

  fun setB(b: Int) {
    this.b = b
  }
}

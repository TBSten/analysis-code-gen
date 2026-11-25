// PATTERN: MSH-T-0076
// TITLE: 実行時テスト / GeneratedAdaptersTest / GetterAndSetter
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#GetterAndSetter
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: GetterAndSetter.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class GetterAndSetter(var a: Int, var b: Int) {
  var total: Int
    get() = a + b
    set(value) {
      b = value - a
    }
}

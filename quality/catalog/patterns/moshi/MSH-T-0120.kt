// PATTERN: MSH-T-0120
// TITLE: 実行時テスト / DualKotlinTest / OnlyMultipleNonPropertyParameters
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/DualKotlinTest.kt#OnlyMultipleNonPropertyParameters
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: OnlyMultipleNonPropertyParameters.kt
package com.squareup.moshi.kotlin

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class OnlyMultipleNonPropertyParameters(
  param1: Int = 1,
  param2: Int = 2
) {
  init {
    // Ensure the params always uses their default value
    require(param1 == 1)
    require(param2 == 2)
  }

  var prop: Int = 0
}

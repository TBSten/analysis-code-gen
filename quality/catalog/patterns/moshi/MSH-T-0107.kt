// PATTERN: MSH-T-0107
// TITLE: 実行時テスト / DualKotlinTest / HasNonNullPropertyDifferentJsonName
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/DualKotlinTest.kt#HasNonNullPropertyDifferentJsonName
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: HasNonNullPropertyDifferentJsonName.kt
package com.squareup.moshi.kotlin

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class HasNonNullPropertyDifferentJsonName {
  @Json(name = "aPrime") var a: String = ""
}

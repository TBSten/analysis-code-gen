// PATTERN: MSH-T-0127
// TITLE: 実行時テスト / DualKotlinTest / PropertyWithDollarSign
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/DualKotlinTest.kt#PropertyWithDollarSign
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: PropertyWithDollarSign.kt
package com.squareup.moshi.kotlin

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PropertyWithDollarSign(
  val `$a`: String,
  @Json(name = "\$b") val b: String
)

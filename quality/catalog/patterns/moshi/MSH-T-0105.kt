// PATTERN: MSH-T-0105
// TITLE: 実行時テスト / DualKotlinTest / RequiredValueWithDifferentJsonNameAbsent
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/DualKotlinTest.kt#RequiredValueWithDifferentJsonNameAbsent
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: RequiredValueWithDifferentJsonNameAbsent.kt
package com.squareup.moshi.kotlin

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class RequiredValueWithDifferentJsonNameAbsent(var a: Int = 3, @Json(name = "bPrime") var b: Int)

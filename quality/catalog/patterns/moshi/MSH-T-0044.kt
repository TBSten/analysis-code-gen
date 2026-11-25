// PATTERN: MSH-T-0044
// TITLE: 実行時テスト / GeneratedAdaptersTest / JsonAnnotation
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#JsonAnnotation
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: JsonAnnotation.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JsonAnnotation(@Json(name = "foo") val bar: String)

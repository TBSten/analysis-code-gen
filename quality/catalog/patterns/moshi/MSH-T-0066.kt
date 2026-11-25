// PATTERN: MSH-T-0066
// TITLE: 実行時テスト / GeneratedAdaptersTest / ConstructorParameterWithJsonName
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#ConstructorParameterWithJsonName
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: ConstructorParameterWithJsonName.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ConstructorParameterWithJsonName(@Json(name = "key a") var a: Int, var b: Int)

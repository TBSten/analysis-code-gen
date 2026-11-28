// PATTERN: MSH-T-0067
// TITLE: 実行時テスト / GeneratedAdaptersTest / PropertyWithJsonName
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#PropertyWithJsonName
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: PropertyWithJsonName.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PropertyWithJsonName {
  @Json(name = "key a") var a: Int = -1
  var b: Int = -1
}

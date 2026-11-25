// PATTERN: MSH-T-0094
// TITLE: 実行時テスト / GeneratedAdaptersTest / ClassWithFieldJson
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter, moshi.shape=nullable-props
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#ClassWithFieldJson
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: ClassWithFieldJson.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ClassWithFieldJson(
  @field:Json(name = "_links") val links: String
) {
  @field:Json(name = "_ids") var ids: String? = null
}

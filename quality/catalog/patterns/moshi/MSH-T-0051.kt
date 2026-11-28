// PATTERN: MSH-T-0051
// TITLE: 実行時テスト / GeneratedAdaptersTest / SpecialCollections
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#SpecialCollections
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: SpecialCollections.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpecialCollections(
  val mutableList: MutableList<String>,
  val mutableSet: MutableSet<String>,
  val mutableMap: MutableMap<String, String>,
  val immutableList: List<String>,
  val immutableSet: Set<String>,
  val immutableMap: Map<String, String>
)

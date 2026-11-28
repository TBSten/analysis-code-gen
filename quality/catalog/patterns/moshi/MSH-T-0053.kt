// PATTERN: MSH-T-0053
// TITLE: 実行時テスト / GeneratedAdaptersTest / NullableTypeParams
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=generic-class, moshi.shape=nested-adapter, moshi.shape=nullable-props
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#NullableTypeParams
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: NullableTypeParams.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NullableTypeParams<T>(
  val nullableList: List<String?>,
  val nullableSet: Set<String?>,
  val nullableMap: Map<String, String?>,
  val nullableT: T?,
  val nonNullT: T
)

// PATTERN: MSH-T-0098
// TITLE: 実行時テスト / GeneratedAdaptersTest / LambdaTypeNames
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#LambdaTypeNames
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: LambdaTypeNames.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LambdaTypeNames(
  val id: String,
  @Transient
  val simple: ((String) -> Boolean)? = null,
  // Receivers count as the first param, just annotated with a special annotation to indicate it's a receiver
  @Transient
  val receiver: (String.(String) -> Boolean)? = null,
  // Tests that we use `FunctionN` since it has more than 23 params
  @Transient
  val arity: (String.(String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String) -> Boolean)? = null,
)

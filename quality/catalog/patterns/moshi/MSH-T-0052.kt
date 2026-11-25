// PATTERN: MSH-T-0052
// TITLE: 実行時テスト / GeneratedAdaptersTest / MutableProperties
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=many-properties, moshi.shape=nested-adapter, moshi.shape=nullable-props
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#MutableProperties
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: MutableProperties.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MutableProperties(
  val immutableProperty: String,
  var mutableProperty: String,
  val immutableMutableList: MutableList<String>,
  val immutableImmutableList: List<String>,
  var mutableMutableList: MutableList<String>,
  var mutableImmutableList: List<String>,
  val nullableImmutableProperty: String?,
  var nullableMutableProperty: String?,
  val nullableImmutableMutableList: MutableList<String>?,
  val nullableImmutableImmutableList: List<String>?,
  var nullableMutableMutableList: MutableList<String>?,
  var nullableMutableImmutableList: List<String>
)

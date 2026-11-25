// PATTERN: MSH-T-0122
// TITLE: 実行時テスト / DualKotlinTest / NullablePrimitives
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=many-properties, moshi.shape=nested-adapter, moshi.shape=nullable-props
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/DualKotlinTest.kt#NullablePrimitives
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: NullablePrimitives.kt
package com.squareup.moshi.kotlin

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NullablePrimitives(
  val objectType: String = "",
  val boolean: Boolean,
  val nullableBoolean: Boolean? = null,
  val byte: Byte,
  val nullableByte: Byte? = null,
  val char: Char,
  val nullableChar: Char? = null,
  val short: Short,
  val nullableShort: Short? = null,
  val int: Int,
  val nullableInt: Int? = null,
  val long: Long,
  val nullableLong: Long? = null,
  val float: Float,
  val nullableFloat: Float? = null,
  val double: Double,
  val nullableDouble: Double? = null
)

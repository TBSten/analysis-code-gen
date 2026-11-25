// PATTERN: MSH-T-0124
// TITLE: 実行時テスト / DualKotlinTest / TypeAliasNullability
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter, moshi.shape=nullable-props
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/DualKotlinTest.kt#TypeAliasNullability
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: TypeAliasNullability.kt
package com.squareup.moshi.kotlin

import com.squareup.moshi.JsonClass

typealias B = NullableA

typealias NullableB = B?

typealias A = Int

typealias NullableA = A?

typealias C = NullableA

typealias D = C

typealias E = D

@Suppress("REDUNDANT_NULLABLE")
@JsonClass(generateAdapter = true)
data class TypeAliasNullability(
  val aShouldBeNonNull: A,
  val nullableAShouldBeNullable: NullableA,
  val redundantNullableAShouldBeNullable: NullableA?,
  val manuallyNullableAShouldBeNullable: A?,
  val convolutedMultiNullableShouldBeNullable: NullableB?,
  val deepNestedNullableShouldBeNullable: E
)

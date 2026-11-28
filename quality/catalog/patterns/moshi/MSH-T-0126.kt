// PATTERN: MSH-T-0126
// TITLE: 実行時テスト / DualKotlinTest / IntersectionTypes
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=generic-class, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/DualKotlinTest.kt#IntersectionTypes
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: IntersectionTypes.kt
package com.squareup.moshi.kotlin

import com.squareup.moshi.JsonClass

interface IntersectionTypeInterface<E : Enum<E>>

typealias A = Int

typealias NullableA = A?

typealias C = NullableA

typealias D = C

typealias E = D

@JsonClass(generateAdapter = true)
data class IntersectionTypes<E>(
  val value: E
) where E : Enum<E>, E : IntersectionTypeInterface<E>

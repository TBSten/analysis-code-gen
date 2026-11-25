// PATTERN: MSH-T-0121
// TITLE: 実行時テスト / DualKotlinTest / TypeAliasUnwrapping
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/DualKotlinTest.kt#TypeAliasUnwrapping
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: TypeAliasUnwrapping.kt
package com.squareup.moshi.kotlin

import com.squareup.moshi.JsonClass

typealias TypeAlias = Int

@Suppress("REDUNDANT_PROJECTION")
typealias GenericTypeAlias = List<out GenericClass<in TypeAlias>?>?

@JsonClass(generateAdapter = true)
data class GenericClass<T>(val value: T)

@JsonClass(generateAdapter = true)
data class TypeAliasUnwrapping(
  val simpleClass: TypeAlias,
  val parameterized: GenericClass<TypeAlias>,
  val wildcardIn: GenericClass<in TypeAlias>,
  val wildcardOut: GenericClass<out TypeAlias>,
  val complex: GenericClass<GenericTypeAlias>?
)

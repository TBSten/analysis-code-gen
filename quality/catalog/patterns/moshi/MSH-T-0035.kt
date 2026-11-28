// PATTERN: MSH-T-0035
// TITLE: 実行時テスト / CompileOnlyTests / SmokeTestType
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=many-properties, moshi.shape=nullable-props
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/CompileOnlyTests.kt#SmokeTestType
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: SmokeTestType.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

typealias TypeAliasName = String

typealias GenericTypeAlias = List<String>

@JsonClass(generateAdapter = true)
data class SmokeTestType(
  @Json(name = "first_name") val firstName: String,
  @Json(name = "last_name") val lastName: String,
  val age: Int,
  val nationalities: List<String> = emptyList(),
  val weight: Float,
  val tattoos: Boolean = false,
  val race: String?,
  val hasChildren: Boolean = false,
  val favoriteFood: String? = null,
  val favoriteDrink: String? = "Water",
  val wildcardOut: MutableList<out String> = mutableListOf(),
  val nullableWildcardOut: MutableList<out String?> = mutableListOf(),
  val wildcardIn: Array<in String>,
  val any: List<*>,
  val anyTwo: List<Any>,
  val anyOut: MutableList<out Any>,
  val nullableAnyOut: MutableList<out Any?>,
  val favoriteThreeNumbers: IntArray,
  val favoriteArrayValues: Array<String>,
  val favoriteNullableArrayValues: Array<String?>,
  val nullableSetListMapArrayNullableIntWithDefault: Set<List<Map<String, Array<IntArray?>>>>? = null,
  val aliasedName: TypeAliasName = "Woah",
  val genericAlias: GenericTypeAlias = listOf("Woah"),
  // Regression test for https://github.com/square/moshi/issues/1272
  val nestedArray: Array<Map<String, Any>>? = null
)

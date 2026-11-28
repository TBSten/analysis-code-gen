// PATTERN: MSH-T-0039
// TITLE: 実行時テスト / ComplexGenericsInheritanceTest / PersonResponse
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=inheritance, moshi.shape=nullable-props
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/ComplexGenericsInheritanceTest.kt#PersonResponse
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: PersonResponse.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

open class ResponseWithSettableProperty<T, R> {
  var data: T? = null
  var data2: R? = null
  var data3: R? = null
}

interface Personable

@JsonClass(generateAdapter = true)
data class Person(val name: String) : Personable

@JsonClass(generateAdapter = true)
data class PersonResponse(
  val extra: String? = null
) : ResponseWithSettableProperty<Person, String>()

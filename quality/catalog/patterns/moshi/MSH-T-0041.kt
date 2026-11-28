// PATTERN: MSH-T-0041
// TITLE: 実行時テスト / ComplexGenericsInheritanceTest / UntypedNestedPersonResponse
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=generic-class, moshi.shape=inheritance, moshi.shape=nullable-props
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/ComplexGenericsInheritanceTest.kt#UntypedNestedPersonResponse
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: UntypedNestedPersonResponse.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

open class ResponseWithSettableProperty<T, R> {
  var data: T? = null
  var data2: R? = null
  var data3: R? = null
}

interface Personable

abstract class NestedResponse<T : Personable> : ResponseWithSettableProperty<T, String>()

@JsonClass(generateAdapter = true)
data class UntypedNestedPersonResponse<T : Personable>(
  val extra: String? = null
) : NestedResponse<T>()

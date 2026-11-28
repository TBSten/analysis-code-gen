// PATTERN: MSH-T-0038
// TITLE: 実行時テスト / ComplexGenericsInheritanceTest / Person
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=inheritance
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/ComplexGenericsInheritanceTest.kt#Person
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Person.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

interface Personable

@JsonClass(generateAdapter = true)
data class Person(val name: String) : Personable

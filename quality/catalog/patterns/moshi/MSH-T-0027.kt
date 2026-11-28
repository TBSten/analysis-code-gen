// PATTERN: MSH-T-0027
// TITLE: 実行時テスト / CompileOnlyTests / GenericTestClassWithDefaults
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=generic-class
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/CompileOnlyTests.kt#GenericTestClassWithDefaults
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: GenericTestClassWithDefaults.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenericTestClassWithDefaults<T>(
  val input: String = "",
  val genericInput: T
)

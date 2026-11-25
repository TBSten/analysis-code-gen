// PATTERN: MSH-T-0043
// TITLE: 実行時テスト / DefaultConstructorTest / TestClass
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/DefaultConstructorTest.kt#TestClass
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: TestClass.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TestClass(
  val required: String,
  val optional: String = "optional",
  val optional2: Int = 2,
  val dynamicSelfReferenceOptional: String = required,
  val dynamicOptional: Int = createInt(),
  val dynamicInlineOptional: Int = createInlineInt()
)

// PATTERN: MSH-T-0029
// TITLE: 実行時テスト / CompileOnlyTests / OtherTestModel
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nullable-props
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/CompileOnlyTests.kt#OtherTestModel
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: OtherTestModel.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TestModel(
  val someVariable: Int,
  val anotherVariable: String
)

@JsonClass(generateAdapter = true)
data class OtherTestModel(val TestModel: TestModel? = null)

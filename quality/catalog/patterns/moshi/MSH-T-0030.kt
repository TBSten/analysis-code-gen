// PATTERN: MSH-T-0030
// TITLE: 実行時テスト / CompileOnlyTests / TestModel
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/CompileOnlyTests.kt#TestModel
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: TestModel.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TestModel(
  val someVariable: Int,
  val anotherVariable: String
)

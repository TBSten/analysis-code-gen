// PATTERN: MSH-T-0034
// TITLE: 実行時テスト / CompileOnlyTests / Hotwords
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/CompileOnlyTests.kt#Hotwords
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Hotwords.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Hotwords(
  val `class`: List<String>?
)

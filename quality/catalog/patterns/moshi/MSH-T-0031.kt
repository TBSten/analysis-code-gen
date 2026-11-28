// PATTERN: MSH-T-0031
// TITLE: 実行時テスト / CompileOnlyTests / MismatchParentAndNestedClassVisibility
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nullable-props
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/CompileOnlyTests.kt#MismatchParentAndNestedClassVisibility
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: MismatchParentAndNestedClassVisibility.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MismatchParentAndNestedClassVisibility(
  val type: Int,
  val name: String? = null
) {

  @JsonClass(generateAdapter = true)
  data class NestedClass(
    val nestedProperty: String
  )
}

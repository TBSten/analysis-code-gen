// PATTERN: MSH-T-0032
// TITLE: 実行時テスト / CompileOnlyTests / NestedClass
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/CompileOnlyTests.kt#NestedClass
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: NestedClass.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NestedClass(
  val nestedProperty: String
)

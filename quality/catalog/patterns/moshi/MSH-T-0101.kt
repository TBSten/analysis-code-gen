// PATTERN: MSH-T-0101
// TITLE: 実行時テスト / MixingReflectAndCodeGen / UsesReflectionAdapter
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/MixingReflectAndCodeGen.kt#UsesReflectionAdapter
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: UsesReflectionAdapter.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
class UsesReflectionAdapter(var a: Int, var b: Int)

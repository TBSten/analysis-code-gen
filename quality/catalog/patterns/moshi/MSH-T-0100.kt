// PATTERN: MSH-T-0100
// TITLE: 実行時テスト / MixingReflectAndCodeGen / UsesGeneratedAdapter
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/MixingReflectAndCodeGen.kt#UsesGeneratedAdapter
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: UsesGeneratedAdapter.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class UsesGeneratedAdapter(var a: Int, var b: Int)

// PATTERN: MSH-T-0069
// TITLE: 実行時テスト / GeneratedAdaptersTest / MultipleTransientConstructorParameters
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#MultipleTransientConstructorParameters
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: MultipleTransientConstructorParameters.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class MultipleTransientConstructorParameters(@Transient var a: Int = -1, var b: Int = -1, @Transient var c: Int = -1)

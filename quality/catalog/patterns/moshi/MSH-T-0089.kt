// PATTERN: MSH-T-0089
// TITLE: 実行時テスト / GeneratedAdaptersTest / HasNullableBoolean
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter, moshi.shape=nullable-props
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#HasNullableBoolean
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: HasNullableBoolean.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HasNullableBoolean(val boolean: Boolean?)

// PATTERN: MSH-T-0077
// TITLE: 実行時テスト / GeneratedAdaptersTest / SubtypeConstructorParameters
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=inheritance, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#SubtypeConstructorParameters
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: SubtypeConstructorParameters.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

open class SupertypeConstructorParameters(var a: Int)

@JsonClass(generateAdapter = true)
class SubtypeConstructorParameters(a: Int, var b: Int) : SupertypeConstructorParameters(a)

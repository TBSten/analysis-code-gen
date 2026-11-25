// PATTERN: MSH-T-0135
// TITLE: 実行時テスト / KotlinJsonAdapterTest / MapOfStringToClassCodegen
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/reflect/KotlinJsonAdapterTest.kt#MapOfStringToClassCodegen
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: MapOfStringToClassCodegen.kt
package com.squareup.moshi.kotlin.reflect

import com.squareup.moshi.JsonClass

class ConstructorParameters(var a: Int, var b: Int)

@JsonClass(generateAdapter = true)
@JvmSuppressWildcards(suppress = false)
data class MapOfStringToClassCodegen(val map: Map<String, ConstructorParameters> = mapOf())

// PATTERN: MSH-T-0134
// TITLE: 実行時テスト / KotlinJsonAdapterTest / MapOfStringToArrayCodegen
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/reflect/KotlinJsonAdapterTest.kt#MapOfStringToArrayCodegen
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: MapOfStringToArrayCodegen.kt
package com.squareup.moshi.kotlin.reflect

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@JvmSuppressWildcards(suppress = false)
data class MapOfStringToArrayCodegen(val map: Map<String, Array<Int>> = mapOf())

// PATTERN: MSH-T-0131
// TITLE: 実行時テスト / KotlinJsonAdapterTest / MapOfStringToEnumCodegen
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/reflect/KotlinJsonAdapterTest.kt#MapOfStringToEnumCodegen
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: MapOfStringToEnumCodegen.kt
package com.squareup.moshi.kotlin.reflect

import com.squareup.moshi.JsonClass

enum class KotlinEnum {
  A, B
}

@JsonClass(generateAdapter = true)
@JvmSuppressWildcards(suppress = false)
data class MapOfStringToEnumCodegen(val map: Map<String, KotlinEnum> = mapOf())

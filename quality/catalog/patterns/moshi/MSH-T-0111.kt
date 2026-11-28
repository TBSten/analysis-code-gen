// PATTERN: MSH-T-0111
// TITLE: 実行時テスト / DualKotlinTest / InlineConsumer
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/DualKotlinTest.kt#InlineConsumer
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: InlineConsumer.kt
package com.squareup.moshi.kotlin

import com.squareup.moshi.JsonClass

@JvmInline
@JsonClass(generateAdapter = true)
value class ValueClass(val i: Int = 0)

@JsonClass(generateAdapter = true)
data class InlineConsumer(val inline: ValueClass)

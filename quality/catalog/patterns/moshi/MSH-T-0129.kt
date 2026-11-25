// PATTERN: MSH-T-0129
// TITLE: 実行時テスト / DualKotlinTest / ValueClass
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/DualKotlinTest.kt#ValueClass
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: ValueClass.kt
package com.squareup.moshi.kotlin

import com.squareup.moshi.JsonClass

@JvmInline
@JsonClass(generateAdapter = true)
value class ValueClass(val i: Int = 0)

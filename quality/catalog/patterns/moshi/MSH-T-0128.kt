// PATTERN: MSH-T-0128
// TITLE: 実行時テスト / DualKotlinTest / GenericClass
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=generic-class
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/DualKotlinTest.kt#GenericClass
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: GenericClass.kt
package com.squareup.moshi.kotlin

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenericClass<T>(val value: T)

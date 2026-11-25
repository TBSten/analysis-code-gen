// PATTERN: MSH-T-0104
// TITLE: 実行時テスト / DualKotlinTest / RequiredValueAbsent
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/DualKotlinTest.kt#RequiredValueAbsent
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: RequiredValueAbsent.kt
package com.squareup.moshi.kotlin

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class RequiredValueAbsent(var a: Int = 3, var b: Int)

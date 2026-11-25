// PATTERN: MSH-T-0118
// TITLE: 実行時テスト / DualKotlinTest / MultipleConstructorsB
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=inheritance, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/DualKotlinTest.kt#MultipleConstructorsB
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: MultipleConstructorsB.kt
package com.squareup.moshi.kotlin

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class MultipleConstructorsA(val f: Int)

@JsonClass(generateAdapter = true)
class MultipleConstructorsB(val f: MultipleConstructorsA = MultipleConstructorsA(5), val b: Int) {
  constructor(f: Int, b: Int = 6) : this(MultipleConstructorsA(f), b)
}

// PATTERN: MSH-T-0102
// TITLE: 実行時テスト / MoshiKspTest / SimpleImpl
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=inheritance, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/MoshiKspTest.kt#SimpleImpl
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: SimpleImpl.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

interface SimpleInterface {
  val a: String
}

@JsonClass(generateAdapter = true)
data class SimpleImpl(override val a: String, val b: String) : Any(), SimpleInterface

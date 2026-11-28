// PATTERN: MSH-T-0080
// TITLE: 実行時テスト / GeneratedAdaptersTest / MultiplePropertiesShareAdapter
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#MultiplePropertiesShareAdapter
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: MultiplePropertiesShareAdapter.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonQualifier

@JsonQualifier
annotation class Uppercase(val inFrench: Boolean, val onSundays: Boolean = false)

@JsonClass(generateAdapter = true)
class MultiplePropertiesShareAdapter(
  @Uppercase(true) var a: String,
  @Uppercase(true) var b: String
)

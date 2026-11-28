// PATTERN: MSH-T-0078
// TITLE: 実行時テスト / GeneratedAdaptersTest / SubtypeProperties
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#SubtypeProperties
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: SubtypeProperties.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

open class SupertypeProperties {
  var a: Int = -1
}

@JsonClass(generateAdapter = true)
class SubtypeProperties : SupertypeProperties() {
  var b: Int = -1
}

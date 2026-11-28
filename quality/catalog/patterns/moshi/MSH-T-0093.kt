// PATTERN: MSH-T-0093
// TITLE: 実行時テスト / GeneratedAdaptersTest / InternalPropertyWithoutBackingField
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#InternalPropertyWithoutBackingField
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: InternalPropertyWithoutBackingField.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class InternalPropertyWithoutBackingField {

  @Transient
  private var foo: Int = 5

  internal var bar
    get() = foo
    set(f) {
      foo = f
    }
}

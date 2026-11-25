// PATTERN: MSH-T-0113
// TITLE: 実行時テスト / DualKotlinTest / Node
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=generic-class, moshi.shape=nested-adapter, moshi.shape=nullable-props
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/DualKotlinTest.kt#Node
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: Node.kt
package com.squareup.moshi.kotlin

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
open class Node<T : Node<T, R>, R : Node<R, T>> {
  // kotlin-reflect doesn't preserve ordering, so put these in alphabetical order so that
  // both reflective and code gen tests work the same
  var r: R? = null
  var t: T? = null
}

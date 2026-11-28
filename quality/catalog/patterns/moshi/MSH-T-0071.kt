// PATTERN: MSH-T-0071
// TITLE: 実行時テスト / GeneratedAdaptersTest / TransientDelegateProperty
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#TransientDelegateProperty
// LICENSE: Apache-2.0
// STATUS: rejected
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している K（KSP2 2.3.14）: K でビルドが失敗した: java.lang.RuntimeException: Exception while generating code for private final fun delegate <T> (initial: T of com.squareup.moshi.kotlin.codegen.TransientDelegateProperty.delegate): kotlin.properties.ReadWriteProperty<kotlin.Any?, T of com.squareup.moshi.kotlin.codegen.TransientDelegateProperty.delegate> declared in com.squareup.moshi.kotlin.codegen.TransientDelegateProperty: FUN name:
// FILE: TransientDelegateProperty.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass
import kotlin.properties.Delegates

@JsonClass(generateAdapter = true)
class TransientDelegateProperty {

  private fun <T> delegate(initial: T) = Delegates.observable(initial) { _, _, _ -> }

  @delegate:Transient var a: Int by delegate(-1)
  @delegate:Transient private var b: Int by delegate(-1)
  var c: Int by delegate(-1)

  @JvmName("getBPublic")
  fun getB() = b
  @JvmName("setBPublic")
  fun setB(b: Int) {
    this.b = b
  }
}

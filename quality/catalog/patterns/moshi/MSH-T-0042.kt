// PATTERN: MSH-T-0042
// TITLE: 実行時テスト / ComplexGenericsInheritanceTest / Layer4
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=generic-class, moshi.shape=inheritance, moshi.shape=nullable-props
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/ComplexGenericsInheritanceTest.kt#Layer4
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Layer4.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass

interface Personable

interface LayerInterface<I>

abstract class Layer1<A> {
  var layer1: A? = null
}

abstract class Layer2<B> : Layer1<B>(), LayerInterface<B> {
  var layer2: B? = null
}

abstract class Layer3<C, D> : Layer2<D>() {
  var layer3C: C? = null
  var layer3D: D? = null
}

@JsonClass(generateAdapter = true)
data class Layer4<E : Personable, F>(
  val layer4E: E,
  val layer4F: F? = null
) : Layer3<List<Int>, String>(), LayerInterface<String>

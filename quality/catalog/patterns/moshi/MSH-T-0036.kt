// PATTERN: MSH-T-0036
// TITLE: 実行時テスト / CompileOnlyTests / ClassWithQualifier
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/CompileOnlyTests.kt#ClassWithQualifier
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: ClassWithQualifier.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonQualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

@Retention(RUNTIME)
@JsonQualifier
annotation class UpperCase(val foo: Array<Foo>)

enum class Foo { BAR }

@JsonClass(generateAdapter = true)
data class ClassWithQualifier(
  @UpperCase(foo = [Foo.BAR])
  val a: Int
)

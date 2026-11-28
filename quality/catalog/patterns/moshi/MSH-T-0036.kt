// PATTERN: MSH-T-0036
// TITLE: 実行時テスト / CompileOnlyTests / ClassWithQualifier
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/CompileOnlyTests.kt#ClassWithQualifier
// LICENSE: Apache-2.0
// STATUS: rejected
// NOTE: K（KSP2 2.3.14）: K でビルドが失敗した: Error preparing ClassWithQualifier: jdk.proxy1/jdk.proxy1.$Proxy42.value(Unknown Source) com.squareup.moshi.kotlin.codegen.ksp.MoshiApiUtilKt.generator(MoshiApiUtil.kt:77) com.squareup.moshi.kotlin.codegen.ksp.JsonClassSymbolProcessor.adapterGenerator(JsonClassSymbolProcessorProvider.kt:122) com.squareup.moshi.kotlin.codegen.ksp.JsonClassSymbolProcessor.process(JsonClassSymbolProcesso
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

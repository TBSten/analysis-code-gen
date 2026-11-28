// PATTERN: MSH-T-0028
// TITLE: 実行時テスト / CompileOnlyTests / TypeAnnotationClass
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/CompileOnlyTests.kt#TypeAnnotationClass
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: TypeAnnotationClass.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass
import kotlin.annotation.AnnotationTarget.TYPE

@Target(TYPE)
annotation class TypeAnnotation

@JsonClass(generateAdapter = true)
data class TypeAnnotationClass(
  val propertyWithAnnotatedType: @TypeAnnotation String = "",
  val generic: List<@TypeAnnotation String>
)

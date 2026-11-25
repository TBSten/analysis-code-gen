// PATTERN: MSH-T-0022
// TITLE: processor テスト / nonFieldApplicableQualifier
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#nonFieldApplicableQualifier
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonQualifier
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.Retention
import kotlin.annotation.Target

@Retention(RUNTIME)
@Target(PROPERTY)
@JsonQualifier
annotation class UpperCase

@JsonClass(generateAdapter = true)
class ClassWithQualifier(@UpperCase val a: Int)

// PATTERN: MSH-T-0023
// TITLE: processor テスト / nonRuntimeQualifier
// PROCESSORS: moshi
// EXPECT: error
// EXPECT_MESSAGE: JsonQualifier @UpperCase must have RUNTIME retention
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#nonRuntimeQualifier
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonQualifier
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.Retention
import kotlin.annotation.Target

@Retention(BINARY)
@Target(PROPERTY, FIELD)
@JsonQualifier
annotation class UpperCase

@JsonClass(generateAdapter = true)
class ClassWithQualifier(@UpperCase val a: Int)

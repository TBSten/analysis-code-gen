// PATTERN: MSH-T-0016
// TITLE: processor テスト / nonPropertyConstructorParameter
// PROCESSORS: moshi
// EXPECT: error
// EXPECT_MESSAGE: No property for required constructor parameter a
// TAGS: moshi.shape=private-api
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#nonPropertyConstructorParameter
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class NonPropertyConstructorParameter(a: Int, val b: Int)

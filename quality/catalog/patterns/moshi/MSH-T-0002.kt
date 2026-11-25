// PATTERN: MSH-T-0002
// TITLE: processor テスト / privateConstructorParameter
// PROCESSORS: moshi
// EXPECT: error
// EXPECT_MESSAGE: property a is not visible
// TAGS: moshi.shape=private-api
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#privateConstructorParameter
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PrivateConstructorParameter(private var a: Int)

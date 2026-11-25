// PATTERN: MSH-T-0024
// TITLE: processor テスト / invalidGenericSyntaxErrorMessaging
// PROCESSORS: moshi
// EXPECT: error
// EXPECT_MESSAGE: Error preparing ElementEnvelope
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#invalidGenericSyntaxErrorMessaging
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ElementEnvelope(val elements: List)

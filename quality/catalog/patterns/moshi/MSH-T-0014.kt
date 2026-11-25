// PATTERN: MSH-T-0014
// TITLE: processor テスト / requiredTransientConstructorParameterFails
// PROCESSORS: moshi
// EXPECT: error
// EXPECT_MESSAGE: No default value for transient/ignored property a
// TAGS: moshi.shape=unsupported-kind
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#requiredTransientConstructorParameterFails
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class RequiredTransientConstructorParameter(@Transient var a: Int)

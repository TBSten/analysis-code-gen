// PATTERN: MSH-T-0011
// TITLE: processor テスト / privateClassesNotSupported
// PROCESSORS: moshi
// EXPECT: error
// EXPECT_MESSAGE: @JsonClass can't be applied to test.PrivateClass: must be internal or public
// TAGS: moshi.shape=unsupported-kind, moshi.shape=private-api
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#privateClassesNotSupported
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
private class PrivateClass(val a: Int)

// PATTERN: MSH-T-0013
// TITLE: processor テスト / objectExpressionsNotSupported
// PROCESSORS: moshi
// EXPECT: error
// EXPECT_MESSAGE: @JsonClass can't be applied to test.expression: must be a Kotlin class
// TAGS: moshi.shape=unsupported-kind
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#objectExpressionsNotSupported
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
val expression = object : Any() {
  var a = 5
}

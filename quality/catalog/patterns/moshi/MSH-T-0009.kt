// PATTERN: MSH-T-0009
// TITLE: processor テスト / enumClassesNotSupported
// PROCESSORS: moshi
// EXPECT: error
// EXPECT_MESSAGE: @JsonClass with 'generateAdapter = \"true\"' can't be applied to test.KotlinEnum: code gen for enums is not supported or necessary
// TAGS: moshi.shape=unsupported-kind
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#enumClassesNotSupported
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
enum class KotlinEnum {
  A, B
}

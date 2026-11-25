// PATTERN: MSH-T-0008
// TITLE: processor テスト / innerClassesNotSupported
// PROCESSORS: moshi
// EXPECT: error
// EXPECT_MESSAGE: @JsonClass can't be applied to test.Outer.InnerClass: must not be an inner class
// TAGS: moshi.shape=unsupported-kind
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#innerClassesNotSupported
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass

class Outer {
  @JsonClass(generateAdapter = true)
  inner class InnerClass(val a: Int)
}

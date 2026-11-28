// PATTERN: MSH-T-0010
// TITLE: processor テスト / localClassesNotSupported
// PROCESSORS: moshi
// EXPECT: error
// EXPECT_MESSAGE: @JsonClass can't be applied to LocalClass: must not be local
// TAGS: moshi.shape=unsupported-kind
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#localClassesNotSupported
// LICENSE: Apache-2.0
// STATUS: rejected
// NOTE: K（KSP2 2.3.14）: K でビルドが通った（EXPECT: error）
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass

fun outer() {
  @JsonClass(generateAdapter = true)
  class LocalClass(val a: Int)
}

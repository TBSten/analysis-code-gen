// PATTERN: MSH-T-0001
// TITLE: processor テスト / privateConstructor
// PROCESSORS: moshi
// EXPECT: error
// EXPECT_MESSAGE: constructor is not internal or public
// TAGS: moshi.shape=private-api
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#privateConstructor
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PrivateConstructor private constructor(var a: Int, var b: Int) {
  fun a() = a
  fun b() = b
  companion object {
    fun newInstance(a: Int, b: Int) = PrivateConstructor(a, b)
  }
}

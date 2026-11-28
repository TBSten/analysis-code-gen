// PATTERN: MSH-T-0003
// TITLE: processor テスト / privateProperties
// PROCESSORS: moshi
// EXPECT: error
// EXPECT_MESSAGE: property a is not visible
// TAGS: moshi.shape=private-api
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#privateProperties
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PrivateProperties {
  private var a: Int = -1
  private var b: Int = -1
}

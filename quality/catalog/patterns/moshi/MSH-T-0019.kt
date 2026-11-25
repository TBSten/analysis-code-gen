// PATTERN: MSH-T-0019
// TITLE: processor テスト / multipleErrors
// PROCESSORS: moshi
// EXPECT: error
// EXPECT_MESSAGE: property a is not visible
// TAGS: moshi.shape=unsupported-kind
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#multipleErrors
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Class1(private var a: Int, private var b: Int)

@JsonClass(generateAdapter = true)
class Class2(private var c: Int)

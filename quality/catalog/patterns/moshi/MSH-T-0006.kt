// PATTERN: MSH-T-0006
// TITLE: processor テスト / abstractClassesNotSupported
// PROCESSORS: moshi
// EXPECT: error
// EXPECT_MESSAGE: @JsonClass can't be applied to test.AbstractClass: must not be abstract
// TAGS: moshi.shape=unsupported-kind
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#abstractClassesNotSupported
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
abstract class AbstractClass(val a: Int)

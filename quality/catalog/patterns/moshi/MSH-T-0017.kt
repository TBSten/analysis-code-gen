// PATTERN: MSH-T-0017
// TITLE: processor テスト / badGeneratedAnnotation
// PROCESSORS: moshi
// EXPECT: ok
// OPTIONS: moshi.generated=javax.annotation.GeneratedBlerg
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#badGeneratedAnnotation
// LICENSE: Apache-2.0
// STATUS: rejected
// NOTE: K（KSP2 2.3.14）: K でビルドが失敗した: Invalid option value for moshi.generated. Found javax.annotation.GeneratedBlerg, allowable values are [javax.annotation.processing.Generated, javax.annotation.Generated].
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Foo(val a: Int)

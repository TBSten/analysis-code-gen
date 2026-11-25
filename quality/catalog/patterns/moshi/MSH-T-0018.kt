// PATTERN: MSH-T-0018
// TITLE: processor テスト / disableProguardGeneration
// PROCESSORS: moshi
// EXPECT: ok
// OPTIONS: moshi.generateProguardRules=false
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#disableProguardGeneration
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Foo(val a: Int)

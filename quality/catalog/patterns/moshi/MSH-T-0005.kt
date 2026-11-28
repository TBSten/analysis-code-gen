// PATTERN: MSH-T-0005
// TITLE: processor テスト / interfacesDoNotErrorWhenGeneratorNotSet
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#interfacesDoNotErrorWhenGeneratorNotSet
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true, generator="customGenerator")
interface Interface

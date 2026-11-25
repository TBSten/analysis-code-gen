// PATTERN: MSH-T-0020
// TITLE: processor テスト / extendPlatformType
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#extendPlatformType
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
class ExtendsPlatformClass(var a: Int) : Date()

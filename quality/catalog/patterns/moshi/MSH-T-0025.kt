// PATTERN: MSH-T-0025
// TITLE: processor テスト / TypeAliases with the same backing type should share the same adapter
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#TypeAliases with the same backing type should share the same adapter
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass

typealias FirstName = String
typealias LastName = String

@JsonClass(generateAdapter = true)
data class Person(val firstName: FirstName, val lastName: LastName, val hairColor: String)

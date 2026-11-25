// PATTERN: MSH-T-0026
// TITLE: processor テスト / Processor should generate comprehensive proguard rules
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#Processor should generate comprehensive proguard rules
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: source.kt
package testPackage
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonQualifier

typealias FirstName = String
typealias LastName = String

@JsonClass(generateAdapter = true)
data class Aliases(val firstName: FirstName, val lastName: LastName, val hairColor: String)

@JsonClass(generateAdapter = true)
data class Simple(val firstName: String)

@JsonClass(generateAdapter = true)
data class Generic<T>(val firstName: T, val lastName: String)

@JsonQualifier
annotation class MyQualifier

@JsonClass(generateAdapter = true)
data class UsingQualifiers(val firstName: String, @MyQualifier val lastName: String)

@JsonClass(generateAdapter = true)
data class MixedTypes(val firstName: String, val otherNames: MutableList<String>)

@JsonClass(generateAdapter = true)
data class DefaultParams(val firstName: String = "")

@JsonClass(generateAdapter = true)
data class Complex<T>(val firstName: FirstName = "", @MyQualifier val names: MutableList<String>, val genericProp: T)

object NestedType {
  @JsonQualifier
  annotation class NestedQualifier

  @JsonClass(generateAdapter = true)
  data class NestedSimple(@NestedQualifier val firstName: String)
}

@JsonClass(generateAdapter = true)
class MultipleMasks(
    val arg0: Long = 0,
    val arg1: Long = 1,
    val arg2: Long = 2,
    val arg3: Long = 3,
    val arg4: Long = 4,
    val arg5: Long = 5,
    val arg6: Long = 6,
    val arg7: Long = 7,
    val arg8: Long = 8,
    val arg9: Long = 9,
    val arg10: Long = 10,
    val arg11: Long,
    val arg12: Long = 12,
    val arg13: Long = 13,
    val arg14: Long = 14,
    val arg15: Long = 15,
    val arg16: Long = 16,
    val arg17: Long = 17,
    val arg18: Long = 18,
    val arg19: Long = 19,
    @Suppress("UNUSED_PARAMETER") arg20: Long = 20,
    val arg21: Long = 21,
    val arg22: Long = 22,
    val arg23: Long = 23,
    val arg24: Long = 24,
    val arg25: Long = 25,
    val arg26: Long = 26,
    val arg27: Long = 27,
    val arg28: Long = 28,
    val arg29: Long = 29,
    val arg30: Long = 30,
    val arg31: Long = 31,
    val arg32: Long = 32,
    val arg33: Long = 33,
    val arg34: Long = 34,
    val arg35: Long = 35,
    val arg36: Long = 36,
    val arg37: Long = 37,
    val arg38: Long = 38,
    @Transient val arg39: Long = 39,
    val arg40: Long = 40,
    val arg41: Long = 41,
    val arg42: Long = 42,
    val arg43: Long = 43,
    val arg44: Long = 44,
    val arg45: Long = 45,
    val arg46: Long = 46,
    val arg47: Long = 47,
    val arg48: Long = 48,
    val arg49: Long = 49,
    val arg50: Long = 50,
    val arg51: Long = 51,
    val arg52: Long = 52,
    @Transient val arg53: Long = 53,
    val arg54: Long = 54,
    val arg55: Long = 55,
    val arg56: Long = 56,
    val arg57: Long = 57,
    val arg58: Long = 58,
    val arg59: Long = 59,
    val arg60: Long = 60,
    val arg61: Long = 61,
    val arg62: Long = 62,
    val arg63: Long = 63,
    val arg64: Long = 64,
    val arg65: Long = 65
)

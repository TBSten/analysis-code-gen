// PATTERN: MSH-T-0020
// TITLE: processor テスト / extendPlatformType
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#extendPlatformType
// LICENSE: Apache-2.0
// STATUS: rejected
// NOTE: K（KSP2 2.3.14）: K でビルドが失敗した: /tmp/acg-diff-k3919944199844863434/sources/source.kt:6: @JsonClass can't be applied to ExtendsPlatformClass: supertype java.util.Date is not a Kotlin type. Origin=JAVA_LIB Annotations=[]
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
class ExtendsPlatformClass(var a: Int) : Date()

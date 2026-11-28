// PATTERN: MSH-T-0021
// TITLE: processor テスト / extendJavaType
// PROCESSORS: moshi
// EXPECT: error
// EXPECT_MESSAGE: supertype com.squareup.moshi.kotlin.codegen.JavaSuperclass is not a Kotlin type
// ORIGIN: test:square/moshi@7f957b18ea12/moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt#extendJavaType
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: source.kt
package test
import com.squareup.moshi.JsonClass
import com.squareup.moshi.kotlin.codegen.JavaSuperclass

@JsonClass(generateAdapter = true)
class ExtendsJavaType(var b: Int) : JavaSuperclass()
// FILE: JavaSuperclass.java
package com.squareup.moshi.kotlin.codegen;
public class JavaSuperclass {
  public int a = 1;
}

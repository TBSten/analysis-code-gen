// PATTERN: MSH-T-0037
// TITLE: 実行時テスト / CompileOnlyTests / DataClassInModuleB
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=inheritance
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/CompileOnlyTests.kt#DataClassInModuleB
// LICENSE: Apache-2.0
// STATUS: rejected
// NOTE: K（KSP2 2.3.14）: K でビルドが失敗した: DataClassInModuleB.kt:9: Unresolved reference 'AbstractClassInModuleA'.
// FILE: DataClassInModuleB.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass
import com.squareup.moshi.kotlin.codegen.test.extra.AbstractClassInModuleA

@JsonClass(generateAdapter = true)
data class DataClassInModuleB(
  val id: String
) : AbstractClassInModuleA()

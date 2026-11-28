// PATTERN: MSH-T-0099
// TITLE: 実行時テスト / ClassInPackageThatLooksLikeAClass / ClassInPackageThatLooksLikeAClass
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/LooksLikeAClass/ClassInPackageThatLooksLikeAClass.kt#ClassInPackageThatLooksLikeAClass
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: ClassInPackageThatLooksLikeAClass.kt
package com.squareup.moshi.kotlin.codegen.LooksLikeAClass

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ClassInPackageThatLooksLikeAClass(val foo: String)

// PATTERN: MSH-T-0064
// TITLE: 実行時テスト / GeneratedAdaptersTest / ConstructorParameterWithQualifierInAnnotationPackage
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/GeneratedAdaptersTest.kt#ConstructorParameterWithQualifierInAnnotationPackage
// LICENSE: Apache-2.0
// STATUS: rejected
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している K（KSP2 2.3.14）: K でビルドが失敗した: Unresolved reference 'annotation'.
// FILE: ConstructorParameterWithQualifierInAnnotationPackage.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.JsonClass
import com.squareup.moshi.kotlin.codegen.annotation.UppercaseInAnnotationPackage

@JsonClass(generateAdapter = true)
class ConstructorParameterWithQualifierInAnnotationPackage(@UppercaseInAnnotationPackage var a: String)

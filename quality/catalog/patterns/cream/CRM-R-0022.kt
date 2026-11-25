// PATTERN: CRM-R-0022
// TITLE: cream.kt の test モジュール / Generics
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineTo/Generics.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineTo/Generics.kt
package me.tbsten.cream.test.combineTo

import me.tbsten.cream.CombineTo

// Generics test
@CombineTo(GenericTarget::class)
data class GenericSourceA<T>(
    val genericProperty: T,
)

@CombineTo(GenericTarget::class)
data class GenericSourceB(
    val normalProperty: String,
)

data class GenericTarget<T>(
    val genericProperty: T,
    val normalProperty: String,
)

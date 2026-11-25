// PATTERN: CRM-R-0020
// TITLE: cream.kt の test モジュール / Basic
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineTo/Basic.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineTo/Basic.kt
package me.tbsten.cream.test.combineTo

import me.tbsten.cream.CombineTo

@CombineTo(TargetState::class)
data class SourceStateA(
    val propertyA: String,
)

@CombineTo(TargetState::class)
data class SourceStateB(
    val propertyB: Int,
)

data class TargetState(
    val propertyA: String,
    val propertyB: Int,
    val propertyC: Boolean,
)

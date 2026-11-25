// PATTERN: CRM-R-0026
// TITLE: cream.kt の test モジュール / Overlap
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineTo/Overlap.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineTo/Overlap.kt
package me.tbsten.cream.test.combineTo

import me.tbsten.cream.CombineTo

// Overlapping properties with different types test
@CombineTo(OverlapTarget::class)
data class OverlapSourceA(
    val sharedProperty: String,
    val uniqueA: Int,
)

@CombineTo(OverlapTarget::class)
data class OverlapSourceB(
    val sharedProperty: String,
    val uniqueB: Boolean,
)

data class OverlapTarget(
    val sharedProperty: String,
    val uniqueA: Int,
    val uniqueB: Boolean,
)

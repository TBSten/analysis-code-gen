// PATTERN: CRM-R-0009
// TITLE: cream.kt の test モジュール / Overlap
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineFrom/Overlap.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineFrom/Overlap.kt
package me.tbsten.cream.test.combineFrom

import me.tbsten.cream.CombineFrom

/**
 * Test overlapping properties from multiple sources
 * When multiple sources have the same property name,
 * the last source class (in declaration order) should take precedence
 */

data class OverlapSourceA(
    val shared: String,
    val uniqueA: String,
)

data class OverlapSourceB(
    val shared: String,
    val uniqueB: Int,
)

@CombineFrom(OverlapSourceA::class, OverlapSourceB::class)
data class OverlapTarget(
    val shared: String,
    val uniqueA: String,
    val uniqueB: Int,
)

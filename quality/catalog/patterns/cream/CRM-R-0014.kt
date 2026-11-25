// PATTERN: CRM-R-0014
// TITLE: cream.kt の test モジュール / Exclude
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineMapping/Exclude.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineMapping/Exclude.kt
package me.tbsten.cream.test.combineMapping

import me.tbsten.cream.CombineMapping

/**
 * First source model for testing `excludes` (simulates a model from an external library)
 */
data class ExcludePartA(
    val shareProp: String,
    val aProp: Int,
)

/**
 * Second source model for testing `excludes`
 */
data class ExcludePartB(
    val bProp: Boolean,
)

/**
 * Target model combining ExcludePartA and ExcludePartB
 */
data class ExcludeCombined(
    val shareProp: String,
    val aProp: Int,
    val bProp: Boolean,
)

/**
 * `excludes = ["shareProp"]` drops the auto-copy default of `shareProp`, making it a
 * required parameter of the generated combine function.
 */
@CombineMapping(
    sources = [ExcludePartA::class, ExcludePartB::class],
    target = ExcludeCombined::class,
    excludes = ["shareProp"],
)
private object ExcludeMapping

// PATTERN: CRM-R-0017
// TITLE: cream.kt の test モジュール / Overlap
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineMapping/Overlap.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineMapping/Overlap.kt
package me.tbsten.cream.test.combineMapping

import me.tbsten.cream.CombineMapping

/**
 * Source model D for overlap testing
 */
data class LibDModel(
    val sharedName: String,
    val specificD: String,
)

/**
 * Source model E for overlap testing
 */
data class LibEModel(
    val sharedName: String,
    val specificE: Int,
)

/**
 * Target model with overlapping property names
 */
data class OverlapTargetModel(
    val sharedName: String, // Both LibDModel and LibEModel have this property
    val valueA: String,
    val valueB: Int,
)

/**
 * Combine mapping with overlapping property names
 *
 * When multiple sources have the same property name, the last source takes precedence.
 */
@CombineMapping(
    sources = [LibDModel::class, LibEModel::class],
    target = OverlapTargetModel::class,
    properties = [
        CombineMapping.Map(source = "specificD", target = "valueA"),
        CombineMapping.Map(source = "specificE", target = "valueB"),
    ],
)
private object OverlapMapping

// PATTERN: CRM-R-0016
// TITLE: cream.kt の test モジュール / MultiSource
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineMapping/MultiSource.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineMapping/MultiSource.kt
package me.tbsten.cream.test.combineMapping

import me.tbsten.cream.CombineMapping

/**
 * Library C model (simulates a model from yet another external library)
 * Cannot be modified as it's from a library.
 */
data class LibCModel(
    val nameC: String,
    val valueC: Boolean,
)

/**
 * Target model that combines three source classes
 */
data class TripleCombinedModel(
    val nameA: String,
    val valueA: Int,
    val nameB: String,
    val valueB: Double,
    val nameC: String,
    val valueC: Boolean,
)

/**
 * Combine mapping with three source classes
 *
 * Generates: fun LibAModel.copyToTripleCombinedModel(
 *     libBModel: LibBModel,
 *     libCModel: LibCModel
 * ): TripleCombinedModel
 */
@CombineMapping(
    sources = [LibAModel::class, LibBModel::class, LibCModel::class],
    target = TripleCombinedModel::class,
)
private object MultiSourceMapping

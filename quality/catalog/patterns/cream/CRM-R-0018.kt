// PATTERN: CRM-R-0018
// TITLE: cream.kt の test モジュール / PropertyMapping
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineMapping/PropertyMapping.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineMapping/PropertyMapping.kt
package me.tbsten.cream.test.combineMapping

import me.tbsten.cream.CombineMapping

/**
 * Target model with property name differences from source models
 */
data class RenamedTargetModel(
    val targetNameA: String, // maps from LibAModel.nameA
    val targetValueA: Int, // maps from LibAModel.valueA
    val targetNameB: String, // maps from LibBModel.nameB
    val targetValueB: Double, // maps from LibBModel.valueB
    val extra: String,
)

/**
 * Combine mapping with property name mapping
 *
 * Maps properties with different names using CombineMapping.Map
 */
@CombineMapping(
    sources = [LibAModel::class, LibBModel::class],
    target = RenamedTargetModel::class,
    properties = [
        CombineMapping.Map(source = "nameA", target = "targetNameA"),
        CombineMapping.Map(source = "valueA", target = "targetValueA"),
        CombineMapping.Map(source = "nameB", target = "targetNameB"),
        CombineMapping.Map(source = "valueB", target = "targetValueB"),
    ],
)
private object PropertyMapping

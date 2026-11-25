// PATTERN: CRM-R-0031
// TITLE: cream.kt の test モジュール / Basic
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyFrom/Basic.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyFrom/Basic.kt
package me.tbsten.cream.test.copyFrom

import me.tbsten.cream.CopyFrom

/**
 * Data layer model.
 *
 * @property prop1
 * @property prop2
 */
@CopyFrom(DomainLayerModel::class)
data class DataLayerModel(
    val prop1: String,
    val prop2: Int,
)

/**
 * Domain layer model.
 *
 * @property prop1
 * @property prop2
 */
@CopyFrom(DataLayerModel::class)
data class DomainLayerModel(
    val prop1: String,
    val prop2: Int,
)

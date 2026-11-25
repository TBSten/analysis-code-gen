// PATTERN: CRM-R-0039
// TITLE: cream.kt の test モジュール / TypeAlias
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyFrom/TypeAlias.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyFrom/TypeAlias.kt
package me.tbsten.cream.test.copyFrom

import me.tbsten.cream.CopyFrom

data class SourceModel(
    val value: String,
)

data class TargetModel(
    val value: String,
)

typealias SourceModelAlias = SourceModel

@CopyFrom(SourceModelAlias::class) // will generate SourceModel.copyToTargetModel(...): TargetModel
typealias TargetModelAlias = TargetModel

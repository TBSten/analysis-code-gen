// PATTERN: CRM-R-0060
// TITLE: cream.kt の test モジュール / TypeAlias
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyTo/TypeAlias.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyTo/TypeAlias.kt
package me.tbsten.cream.test.copyTo

import me.tbsten.cream.CopyTo

data class DomainModel(
    val id: String,
)

data class DataModel(
    val id: String,
)

typealias DataModelAlias = DataModel

@CopyTo(DataModelAlias::class) // will generate DomainModel.copyToDataModel(...): DataModel
typealias DomainModelAlias = DomainModel

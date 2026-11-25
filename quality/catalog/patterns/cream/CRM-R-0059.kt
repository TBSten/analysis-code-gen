// PATTERN: CRM-R-0059
// TITLE: cream.kt の test モジュール / PropertyMapping
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyTo/PropertyMapping.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyTo/PropertyMapping.kt
package me.tbsten.cream.test.copyTo

import me.tbsten.cream.CopyTo

// Property mapping with @CopyTo.Map
@CopyTo(DataTargetModel::class, UiTargetModel::class)
data class DomainSourceModel(
    @CopyTo.Map("dataId", "uiId")
    val domainId: String,
    val name: String,
)

data class DataTargetModel(
    val dataId: String,
    val name: String,
)

data class UiTargetModel(
    val uiId: String,
    val name: String,
)

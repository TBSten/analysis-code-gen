// PATTERN: CRM-R-0069
// TITLE: cream.kt の test モジュール / PropertyMapping
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyToChildren/PropertyMapping.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyToChildren/PropertyMapping.kt
package me.tbsten.cream.test.copyToChildren

import me.tbsten.cream.CopyToChildren

// Property mapping with @CopyToChildren.Map
@CopyToChildren
sealed interface MappedState {
    @CopyToChildren.Map("loadingId", "successId")
    val stateId: String
    val name: String
}

data class MappedLoading(
    val loadingId: String,
    override val name: String,
) : MappedState {
    override val stateId: String get() = loadingId
}

data class MappedSuccess(
    val successId: String,
    override val name: String,
    val data: String,
) : MappedState {
    override val stateId: String get() = successId
}

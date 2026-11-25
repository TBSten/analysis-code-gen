// PATTERN: CRM-R-0012
// TITLE: cream.kt の test モジュール / Visibility
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineFrom/Visibility.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineFrom/Visibility.kt
package me.tbsten.cream.test.combineFrom

import me.tbsten.cream.CombineFrom
import me.tbsten.cream.CopyVisibility

data class InternalVisibilityCombineSource(
    val shared: String,
)

// Generated combine function is forced to `internal` via the visibility argument.
@CombineFrom(InternalVisibilityCombineSource::class, visibility = CopyVisibility.INTERNAL)
data class InternalVisibilityCombineTarget(
    val shared: String,
    val extra: Int,
)

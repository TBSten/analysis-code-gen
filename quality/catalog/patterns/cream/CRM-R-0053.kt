// PATTERN: CRM-R-0053
// TITLE: cream.kt の test モジュール / CopyVisibility
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyTo/CopyVisibility.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyTo/CopyVisibility.kt
package me.tbsten.cream.test.copyTo

import me.tbsten.cream.CopyTo
import me.tbsten.cream.CopyVisibility

// Generated copy function is forced to `internal` via the visibility argument.
@CopyTo(InternalVisibilityTarget::class, visibility = CopyVisibility.INTERNAL)
data class InternalVisibilitySource(
    val shared: String,
)

data class InternalVisibilityTarget(
    val shared: String,
    val extra: Int,
)

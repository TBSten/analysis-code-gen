// PATTERN: CRM-R-0074
// TITLE: cream.kt の test モジュール / Basic
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/sealedCopy/Basic.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/sealedCopy/Basic.kt
package me.tbsten.cream.test.sealedCopy

import me.tbsten.cream.SealedCopy

@SealedCopy
sealed interface BasicState {
    val sessionId: String
    val attempt: Int

    data class Loading(
        override val sessionId: String,
        override val attempt: Int,
    ) : BasicState

    data class Success(
        override val sessionId: String,
        override val attempt: Int,
        val payload: String,
    ) : BasicState
}

// PATTERN: CRM-R-0077
// TITLE: cream.kt の test モジュール / Via
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/sealedCopy/Via.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/sealedCopy/Via.kt
package me.tbsten.cream.test.sealedCopy

import me.tbsten.cream.SealedCopy

/**
 * Exercises `@SealedCopy.Via` on a non-`data class` subtype whose delegate does **not** accept every abstract
 * property under its own name: `attempt` is delegated to a differently-named parameter via `@SealedCopy.Map`.
 *
 * Before issue #162 this generated `is Error -> this.copy(sessionId = sessionId, attempt = attempt)`, which did
 * not match `Error`'s member and fell back to the generated extension itself — infinite recursion
 * (`StackOverflowError`) at runtime. The fix builds the call from the delegate's own parameter names.
 */
@SealedCopy
sealed interface ViaState {
    val sessionId: String
    val attempt: Int

    data class Loading(
        override val sessionId: String,
        override val attempt: Int,
    ) : ViaState

    class Error(
        override val sessionId: String,
        override val attempt: Int,
    ) : ViaState {
        @SealedCopy.Via
        fun cloneWith(
            sessionId: String,
            @SealedCopy.Map("attempt") newAttempt: Int,
        ): Error = Error(sessionId = sessionId, attempt = newAttempt)
    }
}

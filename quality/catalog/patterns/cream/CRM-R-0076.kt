// PATTERN: CRM-R-0076
// TITLE: cream.kt の test モジュール / Multiple
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/sealedCopy/Multiple.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/sealedCopy/Multiple.kt
package me.tbsten.cream.test.sealedCopy

import me.tbsten.cream.NonCopyableStrategy
import me.tbsten.cream.SealedCopy

/**
 * Exercises `@SealedCopy` being `@Repeatable`: two annotations on the same sealed type
 * should produce two independent extensions whose `funName` and `nonCopyableStrategy`
 * are taken from each annotation site.
 *
 * `withUpdated` uses `RETURN_AS_IS` — `Empty` collapses to itself, function returns
 * non-nullable.
 *
 * `withUpdatedOrNull` uses `RETURN_NULL` — `Empty` collapses to `null`, function
 * widens its return type to nullable.
 */
@SealedCopy(funName = "withUpdated", nonCopyableStrategy = NonCopyableStrategy.RETURN_AS_IS)
@SealedCopy(funName = "withUpdatedOrNull", nonCopyableStrategy = NonCopyableStrategy.RETURN_NULL)
sealed interface MultiSealedCopyState {
    val sessionId: String

    data class Loading(
        override val sessionId: String,
    ) : MultiSealedCopyState

    data object Empty : MultiSealedCopyState {
        override val sessionId: String = "empty"
    }
}

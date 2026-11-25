// PATTERN: CRM-R-0043
// TITLE: cream.kt の test モジュール / CopyVisibility
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyMapping/CopyVisibility.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyMapping/CopyVisibility.kt
package me.tbsten.cream.test.copyMapping

import me.tbsten.cream.CopyMapping
import me.tbsten.cream.CopyVisibility

/**
 * Library models whose generated copy functions are forced to `internal` via the `visibility`
 * argument. With `canReverse = true`, the same visibility must be applied to BOTH the forward
 * (`LibA -> LibB`) and reverse (`LibB -> LibA`) functions.
 */
data class InternalVisibilityLibA(
    val shared: String,
    val aProp: Int,
)

data class InternalVisibilityLibB(
    val shared: String,
    val bProp: Boolean,
)

@CopyMapping(
    InternalVisibilityLibA::class,
    InternalVisibilityLibB::class,
    canReverse = true,
    visibility = CopyVisibility.INTERNAL,
)
private object InternalVisibilityMapping

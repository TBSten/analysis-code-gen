// PATTERN: CRM-R-0046
// TITLE: cream.kt の test モジュール / FunName
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyMapping/FunName.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyMapping/FunName.kt
package me.tbsten.cream.test.copyMapping

import me.tbsten.cream.CopyMapping
import me.tbsten.cream.CopyTargetSimpleName

data class FunNameMapA(
    val shared: String,
)

data class FunNameMapB(
    val shared: String,
    val extra: Int,
)

@CopyMapping(FunNameMapA::class, FunNameMapB::class, funName = "to" + CopyTargetSimpleName)
object FunNameMapping

data class FunNameRevA(
    val shared: String,
)

data class FunNameRevB(
    val shared: String,
)

// canReverse + token: forward and reverse names derive from their own targets.
@CopyMapping(FunNameRevA::class, FunNameRevB::class, canReverse = true, funName = "to" + CopyTargetSimpleName)
object FunNameRevMapping

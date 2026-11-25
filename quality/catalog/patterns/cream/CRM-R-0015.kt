// PATTERN: CRM-R-0015
// TITLE: cream.kt の test モジュール / FunName
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineMapping/FunName.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineMapping/FunName.kt
package me.tbsten.cream.test.combineMapping

import me.tbsten.cream.CombineMapping
import me.tbsten.cream.CopyTargetSimpleName

data class FunNameCmA(
    val a: String,
)

data class FunNameCmB(
    val b: Int,
)

data class FunNameCmDst(
    val a: String,
    val b: Int,
    val extra: Long,
)

@CombineMapping(
    sources = [FunNameCmA::class, FunNameCmB::class],
    target = FunNameCmDst::class,
    funName = "to" + CopyTargetSimpleName,
)
object FunNameCmMapping

// PATTERN: CRM-R-0004
// TITLE: cream.kt の test モジュール / FunName
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineFrom/FunName.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineFrom/FunName.kt
package me.tbsten.cream.test.combineFrom

import me.tbsten.cream.CombineFrom
import me.tbsten.cream.CopyTargetSimpleName

data class FunNameA(
    val a: String,
)

data class FunNameB(
    val b: Int,
)

@CombineFrom(FunNameA::class, FunNameB::class, funName = "build" + CopyTargetSimpleName)
data class FunNameDst(
    val a: String,
    val b: Int,
    val extra: Long,
)

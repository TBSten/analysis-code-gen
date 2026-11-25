// PATTERN: CRM-R-0011
// TITLE: cream.kt の test モジュール / TypeAlias
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineFrom/TypeAlias.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineFrom/TypeAlias.kt
package me.tbsten.cream.test.combineFrom

import me.tbsten.cream.CombineFrom

// Source classes for combining
data class SourceX(
    val propX: String,
)

data class SourceY(
    val propY: Int,
)

// Target class that combines properties from both sources
data class CombinedResult(
    val propX: String,
    val propY: Int,
    val extraProp: String,
)

// Typealiases for sources and target
typealias SourceXAlias = SourceX
typealias SourceYAlias = SourceY
typealias CombinedResultAlias = CombinedResult

// Test case: typealias as annotation target with typealias sources
@CombineFrom(SourceXAlias::class, SourceYAlias::class)
typealias CombinedResultWithAlias = CombinedResult

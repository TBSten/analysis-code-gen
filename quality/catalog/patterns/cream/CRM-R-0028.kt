// PATTERN: CRM-R-0028
// TITLE: cream.kt の test モジュール / TypeAlias
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineTo/TypeAlias.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineTo/TypeAlias.kt
package me.tbsten.cream.test.combineTo

import me.tbsten.cream.CombineTo

// Source classes for combining
data class SourceA(
    val propA: String,
)

data class SourceB(
    val propB: Int,
)

// Target class that combines properties from both sources
data class CombinedTarget(
    val propA: String,
    val propB: Int,
    val extra: String,
)

// Typealiases for sources and target
typealias SourceAAlias = SourceA
typealias SourceBAlias = SourceB
typealias CombinedTargetAlias = CombinedTarget

// Test case 1: typealias as annotation target (source)
@CombineTo(CombinedTargetAlias::class)
typealias SourceAWithAlias = SourceA

@CombineTo(CombinedTargetAlias::class)
typealias SourceBWithAlias = SourceB

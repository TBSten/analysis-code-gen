// PATTERN: CRM-R-0006
// TITLE: cream.kt の test モジュール / MultiSource
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineFrom/MultiSource.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineFrom/MultiSource.kt
package me.tbsten.cream.test.combineFrom

import me.tbsten.cream.CombineFrom

/**
 * Test combining from multiple source classes (4 sources)
 */

data class MultiSourceA(
    val propertyA: String,
)

data class MultiSourceB(
    val propertyB: Int,
)

data class MultiSourceC(
    val propertyC: Boolean,
)

data class MultiSourceD(
    val propertyD: Double,
)

@CombineFrom(
    MultiSourceA::class,
    MultiSourceB::class,
    MultiSourceC::class,
    MultiSourceD::class,
)
data class MultiSourceTarget(
    val propertyA: String,
    val propertyB: Int,
    val propertyC: Boolean,
    val propertyD: Double,
    val extraProperty: String,
)

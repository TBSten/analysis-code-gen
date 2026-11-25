// PATTERN: CRM-R-0023
// TITLE: cream.kt の test モジュール / MultiSource
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineTo/MultiSource.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineTo/MultiSource.kt
package me.tbsten.cream.test.combineTo

import me.tbsten.cream.CombineTo

// Multiple sources (4+) test
@CombineTo(MultiSourceTarget::class)
data class MultiSourceA(
    val propertyA: String,
)

@CombineTo(MultiSourceTarget::class)
data class MultiSourceB(
    val propertyB: Int,
)

@CombineTo(MultiSourceTarget::class)
data class MultiSourceC(
    val propertyC: Boolean,
)

@CombineTo(MultiSourceTarget::class)
data class MultiSourceD(
    val propertyD: Double,
)

data class MultiSourceTarget(
    val propertyA: String,
    val propertyB: Int,
    val propertyC: Boolean,
    val propertyD: Double,
)

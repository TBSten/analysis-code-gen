// PATTERN: CRM-R-0007
// TITLE: cream.kt の test モジュール / Nullable
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineFrom/Nullable.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineFrom/Nullable.kt
package me.tbsten.cream.test.combineFrom

import me.tbsten.cream.CombineFrom

/**
 * Test nullable properties with @CombineFrom
 */

data class NullableSourceA(
    val nullableProperty: String?,
    val nonNullProperty: String,
)

data class NullableSourceB(
    val anotherNullable: Int?,
)

@CombineFrom(NullableSourceA::class, NullableSourceB::class)
data class NullableTarget(
    val nullableProperty: String?,
    val nonNullProperty: String,
    val anotherNullable: Int?,
    val extraProperty: Boolean,
)

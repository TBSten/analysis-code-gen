// PATTERN: CRM-R-0035
// TITLE: cream.kt の test モジュール / Nullable
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyFrom/Nullable.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyFrom/Nullable.kt
package me.tbsten.cream.test.copyFrom

import me.tbsten.cream.CopyFrom

// Non-nullable to nullable properties
@CopyFrom(NonNullableSource::class)
data class NullableTarget(
    val str: String?,
    val num: Int?,
    val bool: Boolean?,
    val list: List<String>?,
    val newProperty: String,
)

data class NonNullableSource(
    val str: String,
    val num: Int,
    val bool: Boolean,
    val list: List<String>,
)

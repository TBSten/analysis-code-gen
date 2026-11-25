// PATTERN: CRM-R-0041
// TITLE: cream.kt の test モジュール / Visibility
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyFrom/Visibility.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyFrom/Visibility.kt
package me.tbsten.cream.test.copyFrom

import me.tbsten.cream.CopyFrom

// Different visibility properties
@CopyFrom(VisibilitySource::class)
data class VisibilityTarget(
    val publicProp: String,
    val newProperty: String,
)

data class VisibilitySource(
    val publicProp: String,
    internal val internalProp: Int,
    private val privateProp: Boolean,
)

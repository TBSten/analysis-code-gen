// PATTERN: CRM-R-0063
// TITLE: cream.kt の test モジュール / Visibility
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyTo/Visibility.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyTo/Visibility.kt
package me.tbsten.cream.test.copyTo

import me.tbsten.cream.CopyTo

// Different visibility properties
@CopyTo(VisibilityTarget::class)
data class VisibilitySource(
    val publicProp: String,
    internal val internalProp: Int,
    private val privateProp: Boolean,
)

data class VisibilityTarget(
    val publicProp: String,
    val newProperty: String,
)

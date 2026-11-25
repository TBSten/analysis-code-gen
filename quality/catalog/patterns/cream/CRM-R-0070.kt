// PATTERN: CRM-R-0070
// TITLE: cream.kt の test モジュール / Visibility
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyToChildren/Visibility.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyToChildren/Visibility.kt
package me.tbsten.cream.test.copyToChildren

import me.tbsten.cream.CopyToChildren

// Different visibility properties
@CopyToChildren
sealed class VisibilityParent {
    abstract val publicProp: String
    internal abstract val internalProp: String
}

data class VisibilityChild1(
    override val publicProp: String,
    internal override val internalProp: String,
) : VisibilityParent()

data class VisibilityChild2(
    override val publicProp: String,
    internal override val internalProp: String,
    val newProperty: String,
) : VisibilityParent()

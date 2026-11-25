// PATTERN: CRM-R-0067
// TITLE: cream.kt の test モジュール / Nested
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyToChildren/Nested.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyToChildren/Nested.kt
package me.tbsten.cream.test.copyToChildren

import me.tbsten.cream.CopyToChildren

// Nested classes
@CopyToChildren
sealed class NestedParent {
    open val parent: ParentClass = ParentClass("parent", 0)
}

data class NestedChild1(
    override val parent: ParentClass,
    val name: String,
) : NestedParent()

data class NestedChild2(
    override val parent: ParentClass,
    val value: Int,
) : NestedParent()

data class ParentClass(
    val name: String,
    val value: Int,
)

// PATTERN: CRM-R-0056
// TITLE: cream.kt の test モジュール / Nested
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyTo/Nested.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyTo/Nested.kt
package me.tbsten.cream.test.copyTo

import me.tbsten.cream.CopyTo

// Nested classes
@CopyTo(NestedTarget::class)
data class NestedSource(
    val parent: ParentClass,
)

data class NestedTarget(
    val parent: ParentClass,
    val newProperty: String,
)

data class ParentClass(
    val name: String,
    val value: Int,
)

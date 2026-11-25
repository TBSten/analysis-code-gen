// PATTERN: CRM-R-0034
// TITLE: cream.kt の test モジュール / Nested
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyFrom/Nested.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyFrom/Nested.kt
package me.tbsten.cream.test.copyFrom

import me.tbsten.cream.CopyFrom

// Nested classes
@CopyFrom(NestedSource::class)
data class NestedTarget(
    val parent: ParentClass,
    val newProperty: String,
)

data class NestedSource(
    val parent: ParentClass,
)

data class ParentClass(
    val name: String,
    val value: Int,
)

// PATTERN: CRM-R-0008
// TITLE: cream.kt の test モジュール / ObjectTarget
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineFrom/ObjectTarget.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineFrom/ObjectTarget.kt
package me.tbsten.cream.test.combineFrom

import me.tbsten.cream.CombineFrom

/**
 * Test combining to object targets with @CombineFrom
 */

data class ObjectSourceA(
    val propertyA: String,
)

data class ObjectSourceB(
    val propertyB: Int,
)

@CombineFrom(ObjectSourceA::class, ObjectSourceB::class)
object TargetObject

// PATTERN: CRM-R-0052
// TITLE: cream.kt の test モジュール / ComplexTypes
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyTo/ComplexTypes.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyTo/ComplexTypes.kt
package me.tbsten.cream.test.copyTo

import me.tbsten.cream.CopyTo

// Complex types
@CopyTo(ComplexTypeTarget::class)
data class ComplexTypeSource(
    val stringList: List<String>,
    val intMap: Map<String, Int>,
    val nullableSet: Set<Double>?,
)

data class ComplexTypeTarget(
    val stringList: List<String>,
    val intMap: Map<String, Int>,
    val nullableSet: Set<Double>?,
    val newProperty: String,
)

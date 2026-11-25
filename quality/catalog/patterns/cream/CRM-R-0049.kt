// PATTERN: CRM-R-0049
// TITLE: cream.kt の test モジュール / CrossPackageModels
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyMapping/lib/CrossPackageModels.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyMapping/lib/CrossPackageModels.kt
package me.tbsten.cream.test.copyMapping.lib

/**
 * Simulated external-library models that live in a different package than the `@CopyMapping` holder
 * (issue #145). The generated copy function must be emitted into the holder's package, not into this
 * one, so it stays discoverable where the mapping is declared.
 */
data class CrossSource(
    val shared: String,
    val sourceOnly: Int,
)

data class CrossTarget(
    val shared: String,
    val targetOnly: Int,
)

// PATTERN: CRM-R-0019
// TITLE: cream.kt の test モジュール / TypeAlias
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineMapping/TypeAlias.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineMapping/TypeAlias.kt
package me.tbsten.cream.test.combineMapping

import me.tbsten.cream.CombineMapping

/**
 * Library model used (via a typealias) as a @CombineMapping source.
 */
data class LibAliasFirstModel(
    val firstName: String,
    val firstValue: Int,
)

/**
 * Library model used (via a typealias) as a @CombineMapping source.
 */
data class LibAliasSecondModel(
    val secondName: String,
    val secondValue: Double,
)

/**
 * Target model that combines both aliased sources, referenced via a typealias.
 */
data class LibAliasCombinedModel(
    val firstName: String,
    val firstValue: Int,
    val secondName: String,
    val secondValue: Double,
    val extraProperty: String,
)

typealias LibAliasFirstSource = LibAliasFirstModel

typealias LibAliasSecondSource = LibAliasSecondModel

typealias LibAliasCombinedTarget = LibAliasCombinedModel

/**
 * Mapping object whose @CombineMapping sources and target are all type aliases.
 *
 * Generates: fun LibAliasFirstModel.copyToLibAliasCombinedModel(
 *     libAliasSecondModel: LibAliasSecondModel, extraProperty: String,
 * ): LibAliasCombinedModel
 */
@CombineMapping(
    sources = [LibAliasFirstSource::class, LibAliasSecondSource::class],
    target = LibAliasCombinedTarget::class,
)
private object AliasCombineMapping

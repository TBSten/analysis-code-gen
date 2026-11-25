// PATTERN: CRM-R-0047
// TITLE: cream.kt の test モジュール / TypeAlias
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyMapping/TypeAlias.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyMapping/TypeAlias.kt
package me.tbsten.cream.test.copyMapping

import me.tbsten.cream.CopyMapping

/**
 * Library model used as the source of a typealias-based @CopyMapping.
 *
 * @property shareProp Shared property between the aliased source and target
 * @property aProp Property specific to LibAliasAModel
 */
data class LibAliasAModel(
    val shareProp: String,
    val aProp: Int,
)

/**
 * Library model used as the target of a typealias-based @CopyMapping.
 *
 * @property shareProp Shared property between the aliased source and target
 * @property bProp Property specific to LibAliasBModel
 */
data class LibAliasBModel(
    val shareProp: String,
    val bProp: Int,
)

typealias LibAliasASource = LibAliasAModel

typealias LibAliasBTarget = LibAliasBModel

/**
 * Mapping object whose @CopyMapping source and target are both type aliases.
 *
 * The generated function name is based on the resolved class (LibAliasBModel),
 * not the alias (LibAliasBTarget): LibAliasAModel.copyToLibAliasBModel(...).
 */
@CopyMapping(LibAliasASource::class, LibAliasBTarget::class)
private object AliasMapping

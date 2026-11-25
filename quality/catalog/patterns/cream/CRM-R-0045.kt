// PATTERN: CRM-R-0045
// TITLE: cream.kt の test モジュール / Exclude
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyMapping/Exclude.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyMapping/Exclude.kt
package me.tbsten.cream.test.copyMapping

import me.tbsten.cream.CopyMapping

/**
 * Source model for testing `excludes` (simulates a model from an external library)
 */
data class ExcludeSourceModel(
    val shareProp: String,
    val sourceProp: Int,
)

/**
 * Target model for testing `excludes`
 */
data class ExcludeTargetModel(
    val shareProp: String,
    val targetProp: Int,
)

/**
 * `excludes = ["shareProp"]` drops the auto-copy default of `shareProp`, making it a
 * required parameter of the generated copy function.
 */
@CopyMapping(
    source = ExcludeSourceModel::class,
    target = ExcludeTargetModel::class,
    excludes = ["shareProp"],
)
private object ExcludeMapping

/**
 * Source model for testing `canReverse` + `properties` + `excludes`
 */
data class ExcludeReverseSource(
    val mappedSource: String,
    val shared: String,
)

/**
 * Target model for testing `canReverse` + `properties` + `excludes`
 */
data class ExcludeReverseTarget(
    val mappedTarget: String,
    val shared: String,
)

/**
 * `excludes` entries are target-side names; with `canReverse = true` the entry is translated
 * through the reversed `properties` mapping, so the reverse function's `mappedSource`
 * parameter is required too.
 */
@CopyMapping(
    source = ExcludeReverseSource::class,
    target = ExcludeReverseTarget::class,
    canReverse = true,
    properties = [CopyMapping.Map(source = "mappedSource", target = "mappedTarget")],
    excludes = ["mappedTarget"],
)
private object ExcludeReverseMapping

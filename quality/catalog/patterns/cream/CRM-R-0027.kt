// PATTERN: CRM-R-0027
// TITLE: cream.kt の test モジュール / PropertyMapping
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineTo/PropertyMapping.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineTo/PropertyMapping.kt
package me.tbsten.cream.test.combineTo

import me.tbsten.cream.CombineTo

// Test case 1: Simple property name mapping

/**
 * Test property mapping with @CombineTo.Map annotation
 */
@CombineTo(MappedTarget::class)
data class MappingSourceA(
    @CombineTo.Map("targetPropertyA")
    val sourcePropertyA: String,
)

@CombineTo(MappedTarget::class)
data class MappingSourceB(
    @CombineTo.Map("targetPropertyB")
    val sourcePropertyB: Int,
)

data class MappedTarget(
    val targetPropertyA: String,
    val targetPropertyB: Int,
    val normalProperty: String,
)

// Test case 2: Multiple property names mapping (same source property maps to multiple target properties)
@CombineTo(MultiMappedTarget::class)
data class MultiMappingSource(
    @CombineTo.Map("targetName1", "targetName2")
    val sourceName: String,
)

@CombineTo(MultiMappedTarget::class)
data class MultiMappingSourceB(
    val otherProp: Int,
)

data class MultiMappedTarget(
    val targetName1: String,
    val targetName2: String,
    val otherProp: Int,
)

// Test case 3: Overlapping property mapping (both name matching and explicit mapping)
@CombineTo(MixedMappingTarget::class)
data class MixedMappingSourceA(
    val directMatch: String,
)

@CombineTo(MixedMappingTarget::class)
data class MixedMappingSourceB(
    @CombineTo.Map("renamedProperty")
    val originalProperty: Int,
)

data class MixedMappingTarget(
    val directMatch: String,
    val renamedProperty: Int,
    val extraProperty: Boolean,
)

// Test case 3: Overlapping property mapping (both name matching and explicit mapping)
@CombineTo(MergeMappingTarget::class)
data class MergeMappingSourceA(
    val sourcePropertyA: String,
)

@CombineTo(MergeMappingTarget::class)
data class MergeMappingSourceB(
    @CombineTo.Map("sourcePropertyA")
    val sourcePropertyB: String,
)

data class MergeMappingTarget(
    val sourcePropertyA: String,
)

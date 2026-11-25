// PATTERN: CRM-R-0010
// TITLE: cream.kt の test モジュール / PropertyMapping
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineFrom/PropertyMapping.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineFrom/PropertyMapping.kt
package me.tbsten.cream.test.combineFrom

import me.tbsten.cream.CombineFrom

// Test case 1: Simple property name mapping

/**
 * Test property mapping with @CombineFrom.Map annotation
 */
data class MappingSourceA(
    val sourcePropertyA: String,
)

data class MappingSourceB(
    val sourcePropertyB: Int,
)

@CombineFrom(MappingSourceA::class, MappingSourceB::class)
data class MappedTarget(
    @CombineFrom.Map("sourcePropertyA")
    val targetPropertyA: String,
    @CombineFrom.Map("sourcePropertyB")
    val targetPropertyB: Int,
    val normalProperty: String,
)

// Test case 2: Multiple property names mapping

data class MultiMappingSource(
    val sourceName: String,
)

data class MultiMappingSourceB(
    val otherProp: Int,
)

@CombineFrom(MultiMappingSource::class, MultiMappingSourceB::class)
data class MultiMappedTarget(
    @CombineFrom.Map("sourceName")
    val targetName1: String,
    @CombineFrom.Map("sourceName")
    val targetName2: String,
    val otherProp: Int,
)

// Test case 3: Mixed mapping (both direct match and explicit mapping)

data class MixedMappingSourceA(
    val directMatch: String,
)

data class MixedMappingSourceB(
    val originalProperty: Int,
)

@CombineFrom(MixedMappingSourceA::class, MixedMappingSourceB::class)
data class MixedMappingTarget(
    val directMatch: String,
    @CombineFrom.Map("originalProperty")
    val renamedProperty: Int,
    val extraProperty: Boolean,
)

// Test case 4: Merge mapping (multiple sources map to same target property)

data class MergeMappingSourceA(
    val sourcePropertyA: String,
)

data class MergeMappingSourceB(
    val sourcePropertyB: String,
)

@CombineFrom(MergeMappingSourceA::class, MergeMappingSourceB::class)
data class MergeMappingTarget(
    @CombineFrom.Map("sourcePropertyB")
    val sourcePropertyA: String,
)

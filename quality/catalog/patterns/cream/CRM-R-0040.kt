// PATTERN: CRM-R-0040
// TITLE: cream.kt の test モジュール / ValueClassMapping
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyFrom/ValueClassMapping.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyFrom/ValueClassMapping.kt
package me.tbsten.cream.test.copyFrom

import me.tbsten.cream.CopyFrom
import kotlin.jvm.JvmInline

@JvmInline
value class AutoMappedId(
    val value: String,
)

// Wrap (this module sets cream.autoValueClassMapping=true): source `id: String` -> target `id: AutoMappedId` gets `= AutoMappedId(this.id)`.
data class ValueClassWrapSource(
    val id: String,
    val name: String,
)

@CopyFrom(ValueClassWrapSource::class)
data class ValueClassWrapTarget(
    val id: AutoMappedId,
    val name: String,
)

// Unwrap (this module sets cream.autoValueClassMapping=true): source `id: AutoMappedId` -> target `id: String` gets `= this.id.value`.
data class ValueClassUnwrapSource(
    val id: AutoMappedId,
    val name: String,
)

@CopyFrom(ValueClassUnwrapSource::class)
data class ValueClassUnwrapTarget(
    val id: String,
    val name: String,
)

// PATTERN: CRM-R-0061
// TITLE: cream.kt の test モジュール / ValueClassMapping
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyTo/ValueClassMapping.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyTo/ValueClassMapping.kt
package me.tbsten.cream.test.copyTo

import me.tbsten.cream.CopyTo
import kotlin.jvm.JvmInline

@JvmInline
value class AutoMappedId(
    val value: String,
)

// Wrap (this module sets cream.autoValueClassMapping=true): source `id: String` -> target `id: AutoMappedId` gets `= AutoMappedId(this.id)`.
@CopyTo(ValueClassWrapTarget::class)
data class ValueClassWrapSource(
    val id: String,
    val name: String,
)

data class ValueClassWrapTarget(
    val id: AutoMappedId,
    val name: String,
)

// Unwrap (this module sets cream.autoValueClassMapping=true): source `id: AutoMappedId` -> target `id: String` gets `= this.id.value`.
@CopyTo(ValueClassUnwrapTarget::class)
data class ValueClassUnwrapSource(
    val id: AutoMappedId,
    val name: String,
)

data class ValueClassUnwrapTarget(
    val id: String,
    val name: String,
)

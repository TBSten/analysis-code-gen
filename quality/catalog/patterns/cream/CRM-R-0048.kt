// PATTERN: CRM-R-0048
// TITLE: cream.kt の test モジュール / ValueClassMapping
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyMapping/ValueClassMapping.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyMapping/ValueClassMapping.kt
package me.tbsten.cream.test.copyMapping

import me.tbsten.cream.CopyMapping
import kotlin.jvm.JvmInline

@JvmInline
value class MappedId(
    val value: String,
)

// Simulates an external library model exposing the raw type...
data class RawIdModel(
    val id: String,
    val name: String,
)

// ...and a domain model wrapping it in a value class.
data class WrappedIdModel(
    val id: MappedId,
    val name: String,
)

// canReverse: the forward function wraps (`MappedId(this.id)`), the generated reverse function
// unwraps (`this.id.value`).
@CopyMapping(RawIdModel::class, WrappedIdModel::class, canReverse = true)
private object ValueClassMappingHolder

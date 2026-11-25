// PATTERN: CRM-R-0029
// TITLE: cream.kt の test モジュール / ValueClassMapping
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/combineTo/ValueClassMapping.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/combineTo/ValueClassMapping.kt
package me.tbsten.cream.test.combineTo

import me.tbsten.cream.CombineTo
import kotlin.jvm.JvmInline

@JvmInline
value class CombinedId(
    val value: String,
)

// Wrap from the receiver source (`= CombinedId(this.id)`) and unwrap from the secondary source
// (`= combineSourceB.serial.value`) in the same generated combine function.
@CombineTo(CombineValueClassTarget::class)
data class CombineSourceA(
    val id: String,
)

@CombineTo(CombineValueClassTarget::class)
data class CombineSourceB(
    val serial: CombinedId,
)

data class CombineValueClassTarget(
    val id: CombinedId,
    val serial: String,
    val extra: Boolean,
)

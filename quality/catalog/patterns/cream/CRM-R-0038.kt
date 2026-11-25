// PATTERN: CRM-R-0038
// TITLE: cream.kt の test モジュール / SealedTarget
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyFrom/SealedTarget.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyFrom/SealedTarget.kt
package me.tbsten.cream.test.copyFrom

import me.tbsten.cream.CopyFrom

/**
 * A sealed `@CopyFrom` target. cream fans out to every transitive concrete leaf
 * (`First.Second.Done` / `First.Second.Refreshing`) and generates one copy function per leaf
 * whose receiver is [SealedTargetSource]. Mirrors the reproduction of issue #144 (the generated
 * KDoc must attribute generation to the `@CopyFrom`-annotated [SealedTargetState], not the source).
 */
@CopyFrom(SealedTargetSource::class)
sealed interface SealedTargetState {
    val name: String

    sealed interface First : SealedTargetState {
        sealed interface Second : First {
            data class Done(
                override val name: String,
            ) : Second

            data class Refreshing(
                override val name: String,
                val extra: Int,
            ) : Second
        }
    }
}

/**
 * Source for [SealedTargetState] copy functions.
 *
 * @property name
 */
data class SealedTargetSource(
    val name: String,
)

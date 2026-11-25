// PATTERN: CRM-R-0066
// TITLE: cream.kt の test モジュール / FunName
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyToChildren/FunName.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyToChildren/FunName.kt
package me.tbsten.cream.test.copyToChildren

import me.tbsten.cream.CopyTargetSimpleName
import me.tbsten.cream.CopyToChildren
import me.tbsten.cream.DefaultCopyFunctionName

// Token override: each per-leaf function name is built from its own leaf's simple name, so every
// generated function stays unique across the sealed hierarchy's leaves.
@CopyToChildren(funName = "into" + CopyTargetSimpleName)
sealed interface FunNameState {
    val id: String

    data class Loading(
        override val id: String,
    ) : FunNameState

    data class Success(
        override val id: String,
        val data: String,
    ) : FunNameState
}

// DefaultCopyFunctionName + suffix: keep cream's derived per-leaf name and append a suffix. The
// derived name already embeds the target, so each leaf's name stays unique.
@CopyToChildren(funName = DefaultCopyFunctionName + "OrNull")
sealed interface FunNameSuffixState {
    val id: String

    data class A(
        override val id: String,
    ) : FunNameSuffixState

    data class B(
        override val id: String,
        val x: Int,
    ) : FunNameSuffixState
}

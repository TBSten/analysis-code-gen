// PATTERN: CRM-R-0054
// TITLE: cream.kt の test モジュール / FunName
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyTo/FunName.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyTo/FunName.kt
package me.tbsten.cream.test.copyTo

import me.tbsten.cream.CopyTargetSimpleName
import me.tbsten.cream.CopyTo

// Token override: the generated name is built from the target's simple name.
@CopyTo(FunNameTarget::class, funName = "to" + CopyTargetSimpleName)
data class FunNameSource(
    val shared: String,
)

data class FunNameTarget(
    val shared: String,
    val extra: Int,
)

// Backtick-quoted literal name (contains a space).
@CopyTo(FunNameBacktickTarget::class, funName = "`to backtick`")
data class FunNameBacktickSource(
    val shared: String,
)

data class FunNameBacktickTarget(
    val shared: String,
)

// Sealed target: a token names each generated child function from its own child.
@CopyTo(FunNameState::class, funName = "into" + CopyTargetSimpleName)
data class FunNameStateSource(
    val id: String,
)

sealed interface FunNameState {
    val id: String

    data class A(
        override val id: String,
    ) : FunNameState

    data class B(
        override val id: String,
        val x: Int,
    ) : FunNameState
}

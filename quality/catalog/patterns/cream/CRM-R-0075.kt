// PATTERN: CRM-R-0075
// TITLE: cream.kt の test モジュール / FunName
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/sealedCopy/FunName.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/sealedCopy/FunName.kt
package me.tbsten.cream.test.sealedCopy

import me.tbsten.cream.CopyTargetSimpleName
import me.tbsten.cream.DefaultCopyFunctionName
import me.tbsten.cream.SealedCopy

// DefaultCopyFunctionName resolves to "copy" for @SealedCopy; the second annotation uses a
// token that renders the sealed type's own name.
@SealedCopy(funName = "my" + DefaultCopyFunctionName)
@SealedCopy(funName = "to" + CopyTargetSimpleName)
sealed interface FunNameSealed {
    val id: String

    data class A(
        override val id: String,
    ) : FunNameSealed

    data class B(
        override val id: String,
        val x: Int,
    ) : FunNameSealed
}

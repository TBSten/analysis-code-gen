// PATTERN: CRM-R-0055
// TITLE: cream.kt の test モジュール / KDoc
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyTo/KDoc.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyTo/KDoc.kt
package me.tbsten.cream.test.copyTo

import me.tbsten.cream.CopyTo
import me.tbsten.cream.KDoc

@CopyTo(
    KDocTarget::class,
    kdoc =
        KDoc(
            description = "This function should not be used in the case of ~.",
            examples = [
                """
                # Sample

                ```kt
                val target = source.copyToKDocTarget(extra = 42)
                ```
                """,
            ],
        ),
)
data class KDocSource(
    val shared: String,
    val onlyOnSource: Int,
)

data class KDocTarget(
    val shared: String,
    val extra: Int,
)

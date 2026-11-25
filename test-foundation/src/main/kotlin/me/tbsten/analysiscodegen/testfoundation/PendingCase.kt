package me.tbsten.analysiscodegen.testfoundation

/**
 * 保留（skip）中のケースに付ける印。設計上の `@PendingCase(id, milestone, reason)` を FreeSpec の DSL 引数で表したもの。
 *
 * 状態の移り変わりは「保留 → 有効」の一方向だけ。一度外した保留を付け直すと `:quality:checkCaseIds` が失敗する。
 */
data class PendingCase(
    val milestone: Milestone,
    val reason: PendingReason = PendingReason.NotImplemented,
)

/** 保留の理由は 2 種類だけ（計画書 14「E2E テストの先行準備と保留の外し方」）。 */
sealed interface PendingReason {
    /** 書式化した理由。状態ファイルの reason 列に出る。 */
    val code: String

    /** まだ実装していない。 */
    data object NotImplemented : PendingReason {
        override val code: String = "NotImplemented"
    }

    /** ADR で決めた理由で保留している（例: 仕様変更で期待結果を見直し中）。 */
    data class Adr(val number: Int) : PendingReason {
        init {
            require(number > 0) { "ADR 番号は 1 以上: $number" }
        }

        override val code: String = "ADR-%03d".format(number)
    }

    companion object {
        fun parse(code: String): PendingReason? =
            when {
                code == NotImplemented.code -> NotImplemented
                code.matches(Regex("ADR-\\d+")) -> Adr(code.removePrefix("ADR-").toInt())
                else -> null
            }
    }
}

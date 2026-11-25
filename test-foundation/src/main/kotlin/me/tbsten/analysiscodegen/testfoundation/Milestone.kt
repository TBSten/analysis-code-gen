package me.tbsten.analysiscodegen.testfoundation

/** ケースが通っていなければならない最初のマイルストーン（一覧の「M」列）。 */
enum class Milestone(val label: String) {
    M1("M1"),
    M2("M2"),
    M3("M3"),

    /** 「任意」。合格ラインに含めない。 */
    Optional("任意"),
    ;

    companion object {
        /** 一覧の M 列（例: `M2`、`M2 / M3`、`M1〜M3`、`任意`）から最初のマイルストーンを読む。 */
        fun parseFirst(raw: String): Milestone? {
            val s = raw.trim()
            if (s.startsWith("任意")) return Optional
            return when (s.take(2)) {
                "M1" -> M1
                "M2" -> M2
                "M3" -> M3
                else -> null
            }
        }

        fun ofLabel(label: String): Milestone? = entries.firstOrNull { it.label == label }
    }
}

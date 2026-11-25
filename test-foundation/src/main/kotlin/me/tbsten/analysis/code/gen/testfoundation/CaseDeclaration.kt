package me.tbsten.analysis.code.gen.testfoundation

/** spec が宣言した 1 本のケーステスト。整合チェックと状態ファイルの元になる。 */
data class CaseDeclaration(
    val id: String,
    val title: String,
    val kind: CaseKind,
    val pending: PendingCase?,
    val specClass: String,
) {
    /** JUnit XML などに出るテスト名。先頭の `[ID]` で集計スクリプトがケースを特定する。 */
    val testName: String get() = testNameOf(id, title)

    companion object {
        /**
         * ケース ID の書式。一覧の `観点-連番`（観点は `RW-ALL` のように複数語も可）、細分化した `a`, `b` 付きも許す。
         * KSP 公式テストは `KSPT-<上流のメソッド名から test を除いたもの>`（例: `KSPT-AllFunctions_kotlin`、ADR-024）。
         */
        val ID_PATTERN: Regex = Regex("^(?:[A-Z]+(-[A-Z]+)*-[A-Z0-9]+[a-z]?|KSPT-[A-Za-z0-9_]+)$")

        fun testNameOf(
            id: String,
            title: String,
        ): String = "[$id] $title"
    }
}

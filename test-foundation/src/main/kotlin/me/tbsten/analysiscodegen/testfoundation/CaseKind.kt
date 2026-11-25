package me.tbsten.analysiscodegen.testfoundation

/** テストケース一覧の「種別」列（quality/test-case-inventory.md 2 章）。 */
enum class CaseKind(val letter: Char, val label: String) {
    Unit('U', "Unit"),
    Integration('I', "Integration"),
    E2E('E', "E2E"),
    Diff('D', "差分"),
    Property('P', "Property-based"),
    Benchmark('B', "ベンチマーク"),
    ;

    companion object {
        fun ofLetter(letter: Char): CaseKind = entries.firstOrNull { it.letter == letter } ?: error("未知の種別: $letter")
    }
}

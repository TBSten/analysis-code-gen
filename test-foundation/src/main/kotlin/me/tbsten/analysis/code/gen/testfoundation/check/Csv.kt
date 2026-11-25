package me.tbsten.analysis.code.gen.testfoundation.check

/** RFC 4180 の範囲の小さな CSV 読み書き（依存を増やさないため自前）。 */
internal object Csv {
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var i = 0
        var quoted = false
        while (i < text.length) {
            val c = text[i]
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        quoted = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> quoted = true
                    ',' -> {
                        row += field.toString()
                        field.clear()
                    }
                    '\r' -> {}
                    '\n' -> {
                        row += field.toString()
                        field.clear()
                        rows += row
                        row = mutableListOf()
                    }
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row += field.toString()
            rows += row
        }
        return rows.filter { r -> !(r.size == 1 && r[0].isEmpty()) }
    }

    /** ヘッダ行を使って列名 → 値の Map にする。 */
    fun parseWithHeader(text: String): List<Map<String, String>> {
        val rows = parse(text)
        if (rows.isEmpty()) return emptyList()
        val header = rows.first()
        return rows.drop(1).map { r -> header.indices.associate { header[it] to r.getOrElse(it) { "" } } }
    }

    fun format(
        header: List<String>,
        rows: List<List<String>>,
    ): String =
        buildString {
            (listOf(header) + rows).forEach { r -> append(r.joinToString(",") { escape(it) }).append('\n') }
        }

    private fun escape(v: String): String =
        if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
}

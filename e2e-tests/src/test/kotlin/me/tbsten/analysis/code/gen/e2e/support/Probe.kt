package me.tbsten.analysis.code.gen.e2e.support

import java.io.File

/**
 * TP-Probe（ADR-027）が `tp.probe.out` に書いた JSON Lines を読む。
 *
 * ```
 * val out = p.dir.resolve("probe.jsonl")
 * p.build("analysisCodeGenMainSourceSet", "-Ptp.probe.out=$out")
 * val events = readProbe(out)
 * events.filter { it.event == "process" }.map { it.round } shouldBe listOf(1, 2)
 * ```
 */
data class ProbeRecord(val raw: Map<String, Any?>) {
    val event: String get() = raw["event"] as String
    val frontend: String get() = raw["frontend"] as String
    val instance: String get() = raw["instance"] as String
    val unit: String? get() = raw["unit"] as String?
    val round: Int? get() = (raw["round"] as Number?)?.toInt()
    val sourceSet: String? get() = raw["sourceSet"] as String?
    val compilation: String? get() = raw["compilation"] as String?
    val platform: String? get() = raw["platform"] as String?
    val options: Map<String, String> get() = strings(raw["options"])
    val allFiles: List<String> get() = list(raw["allFiles"])
    val newFiles: List<String> get() = list(raw["newFiles"])
    val changedFiles: List<String> get() = list(raw["changedFiles"])
    val removedFilePaths: List<String> get() = list(raw["removedFilePaths"])
    val javaFiles: List<String> get() = list(raw["javaFiles"])
    val deferred: List<String> get() = list(raw["deferred"])
    val extra: Map<String, String> get() = strings(raw["extra"])

    private companion object {
        @Suppress("UNCHECKED_CAST")
        fun list(v: Any?): List<String> = (v as List<String>?).orEmpty()

        @Suppress("UNCHECKED_CAST")
        fun strings(v: Any?): Map<String, String> = (v as Map<String, String>?).orEmpty()
    }
}

/** ファイルが無ければ空。 */
fun readProbe(file: File): List<ProbeRecord> =
    if (!file.isFile) {
        emptyList()
    } else {
        file.readLines().filter { it.isNotBlank() }.map { line ->
            @Suppress("UNCHECKED_CAST")
            ProbeRecord(MiniJson(line).parseValue() as Map<String, Any?>)
        }
    }

/** ProbeEvent の JSON（文字列・数・配列・オブジェクト・真偽・null）だけを読む小さな parser。依存を増やさないため自前。 */
internal class MiniJson(private val s: String) {
    private var i = 0

    fun parseValue(): Any? {
        skipWs()
        return when (val c = s[i]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> literal("true", true)
            'f' -> literal("false", false)
            'n' -> literal("null", null)
            else -> if (c == '-' || c.isDigit()) parseNumber() else error("JSON の解析に失敗（位置 $i）: $s")
        }
    }

    private fun parseObject(): Map<String, Any?> {
        val m = linkedMapOf<String, Any?>()
        i++
        skipWs()
        if (s[i] == '}') return m.also { i++ }
        while (true) {
            skipWs()
            val k = parseString()
            skipWs()
            expect(':')
            m[k] = parseValue()
            skipWs()
            if (s[i] == ',') i++ else return m.also { expect('}') }
        }
    }

    private fun parseArray(): List<Any?> {
        val l = mutableListOf<Any?>()
        i++
        skipWs()
        if (s[i] == ']') return l.also { i++ }
        while (true) {
            l += parseValue()
            skipWs()
            if (s[i] == ',') i++ else return l.also { expect(']') }
        }
    }

    private fun parseString(): String {
        expect('"')
        val sb = StringBuilder()
        while (s[i] != '"') {
            val c = s[i++]
            if (c != '\\') {
                sb.append(c)
                continue
            }
            when (val e = s[i++]) {
                'n' -> sb.append('\n')
                'r' -> sb.append('\r')
                't' -> sb.append('\t')
                'u' -> sb.append(s.substring(i, i + 4).toInt(16).toChar()).also { i += 4 }
                else -> sb.append(e)
            }
        }
        i++
        return sb.toString()
    }

    private fun parseNumber(): Number {
        val start = i
        while (i < s.length && (s[i] == '-' || s[i] == '+' || s[i] == '.' || s[i] == 'e' || s[i] == 'E' || s[i].isDigit())) i++
        val t = s.substring(start, i)
        return t.toLongOrNull() ?: t.toDouble()
    }

    private fun literal(
        word: String,
        value: Any?,
    ): Any? {
        check(s.startsWith(word, i)) { "JSON の解析に失敗（位置 $i）: $s" }
        i += word.length
        return value
    }

    private fun expect(c: Char) {
        check(s[i] == c) { "'$c' が来るはず（位置 $i）: $s" }
        i++
    }

    private fun skipWs() {
        while (i < s.length && s[i].isWhitespace()) i++
    }
}

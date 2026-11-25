package me.tbsten.analysis.code.gen.differential

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import me.tbsten.analysis.code.gen.pbt.run.acgCheckAll
import me.tbsten.analysis.code.gen.testfoundation.UnitSpec

private val normalizer = Normalizer.default()

private fun output(
    files: Map<String, String>,
    diagnostics: List<DiagnosticRecord> = emptyList(),
    roots: List<String> = emptyList(),
    success: Boolean = true,
    rounds: List<RoundRecord>? = null,
) = RunOutput(success, files, diagnostics, rounds, roots)

private fun same(
    a: RunOutput,
    b: RunOutput,
): Boolean {
    val na = normalizer.normalize(a)
    val nb = normalizer.normalize(b)
    return na == nb && OutputComparator.compare(na, nb, Aspect.entries.toSet()).isEmpty()
}

// 行の中身に使う文字（空白と改行は別に混ぜる。行頭のインデントは空白で表す）
private val lineChars = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf('(', ')', '{', '}', '.', ':', '"', '=', ' ')
private val lineArb = Arb.list(Arb.element(lineChars), 0..30).map { it.joinToString("") }
private val linesArb = Arb.list(lineArb, 1..12)

class NormalizationTest : UnitSpec({
    case("XD-16", "正規化の規則（パス・順序・改行・埋め込み値・Logger の位置）が、本質的な差を消さずに非本質的な差だけを消す") {
        // ---- 消す: 非本質的な差 ----

        // 改行コード・行末の空白・末尾の空行
        // 固定 seed・回帰の再生は PBT 基盤（ADR-022）に任せる
        acgCheckAll("XD-16 改行と行末の空白の違いは消える", linesArb, prIterations = 200) { lines ->
            val lf = lines.joinToString("\n", postfix = "\n")
            val crlf = lines.joinToString("\r\n") { "$it  " } + "\r\n\r\n"
            same(output(mapOf("kotlin/A.kt" to lf)), output(mapOf("kotlin/A.kt" to crlf))) shouldBe true
        }

        // 実行ごとの絶対パス（作業ディレクトリ）と区切り文字
        same(
            output(
                mapOf("kotlin/A.kt" to "// from /tmp/run-1/sources/A.kt\n"),
                listOf(DiagnosticRecord(DiagnosticRecord.Severity.Warning, "w", "/tmp/run-1/sources/com/a/A.kt:3:9")),
                roots = listOf("/tmp/run-1"),
            ),
            output(
                mapOf("kotlin\\A.kt" to "// from C:\\work\\run-2/sources/A.kt\n"),
                listOf(DiagnosticRecord(DiagnosticRecord.Severity.Warning, "w", "file://C:/work/run-2/sources/com/a/A.kt:3")),
                roots = listOf("C:\\work\\run-2"),
            ),
        ) shouldBe true

        // ファイルの列挙順・診断の並び・ラウンド内の並び
        val files = (1..8).associate { "kotlin/F$it.kt" to "class F$it\n" }
        val diags = (1..4).map { DiagnosticRecord(DiagnosticRecord.Severity.Warning, "m$it", "A.kt:$it") }
        same(
            output(files, diags, rounds = listOf(RoundRecord(1, files.keys.toList()))),
            output(
                files.entries.reversed().associate { it.toPair() },
                diags.reversed(),
                rounds = listOf(RoundRecord(1, files.keys.reversed())),
            ),
        ) shouldBe true

        // 生成日時の埋め込み
        same(
            output(
                mapOf(
                    "java/A.java" to "@javax.annotation.processing.Generated(value = \"x\", date = \"2026-11-26T01:02:03Z\")\nclass A {}\n",
                ),
            ),
            output(
                mapOf(
                    "java/A.java" to "@javax.annotation.processing.Generated(value = \"x\", date = \"2027-01-01T00:00:00Z\")\nclass A {}\n",
                ),
            ),
        ) shouldBe true

        // Logger の位置: 文言の先頭の「パス:行:」と location の書き方の違い、列
        same(
            output(
                emptyMap(),
                listOf(DiagnosticRecord(DiagnosticRecord.Severity.Error, "/r/sources/com/a/A.kt:7:3: bad", null, true)),
                listOf("/r"),
            ),
            output(emptyMap(), listOf(DiagnosticRecord(DiagnosticRecord.Severity.Error, "bad", "com/a/A.kt:7", true))),
        ) shouldBe true

        // ---- 残す: 本質的な差 ----

        // 中身の 1 文字の違い（行頭のインデントを含む）
        acgCheckAll("XD-16 中身の 1 文字の違いは残る", Arb.bind(linesArb, Arb.int(0..1000)) { l, i -> l to i }, prIterations = 200) { (lines, seed) ->
            val text = lines.joinToString("\n", postfix = "\n")
            val positions = text.indices.filter { text[it] != '\n' }
            if (positions.isNotEmpty()) {
                val i = positions[seed % positions.size]
                val changed = text.substring(0, i) + (if (text[i] == 'x') 'y' else 'x') + text.substring(i + 1)
                same(output(mapOf("kotlin/A.kt" to text)), output(mapOf("kotlin/A.kt" to changed))) shouldBe false
            }
        }
        same(output(mapOf("kotlin/A.kt" to "fun a() {\n    b()\n}\n")), output(mapOf("kotlin/A.kt" to "fun a() {\n  b()\n}\n"))) shouldBe
            false
        // 空行の数（末尾以外）
        same(output(mapOf("kotlin/A.kt" to "a\n\nb\n")), output(mapOf("kotlin/A.kt" to "a\nb\n"))) shouldBe false
        // ファイルの有無・パス・出力の種類
        same(output(mapOf("kotlin/A.kt" to "a\n")), output(emptyMap())) shouldBe false
        same(output(mapOf("kotlin/A.kt" to "a\n")), output(mapOf("kotlin/B.kt" to "a\n"))) shouldBe false
        same(output(mapOf("kotlin/A.kt" to "a\n")), output(mapOf("resources/A.kt" to "a\n"))) shouldBe false
        // 空のファイルと改行 1 つだけのファイル（中身が無い同士は同じ扱い）は区別しないが、中身のあるファイルとは区別する
        same(output(mapOf("kotlin/A.kt" to "")), output(mapOf("kotlin/A.kt" to " \n"))) shouldBe true
        same(output(mapOf("kotlin/A.kt" to "")), output(mapOf("kotlin/A.kt" to "a"))) shouldBe false
        // 成否
        same(output(emptyMap(), success = true), output(emptyMap(), success = false)) shouldBe false
        // 診断の重さ・文言・行番号・ファイル・件数
        val base = DiagnosticRecord(DiagnosticRecord.Severity.Error, "bad", "com/a/A.kt:7", true)
        listOf(
            base.copy(severity = DiagnosticRecord.Severity.Warning),
            base.copy(message = "bad!"),
            base.copy(location = "com/a/A.kt:8"),
            base.copy(location = "com/b/A.kt:7"),
        ).forEach { other -> same(output(emptyMap(), listOf(base)), output(emptyMap(), listOf(other))) shouldBe false }
        same(output(emptyMap(), listOf(base)), output(emptyMap(), listOf(base, base))) shouldBe false
        // ラウンドの区切り（同じファイルでも出たラウンドが違えば違う）
        same(
            output(emptyMap(), rounds = listOf(RoundRecord(1, listOf("kotlin/A.kt", "kotlin/B.kt")))),
            output(emptyMap(), rounds = listOf(RoundRecord(1, listOf("kotlin/A.kt")), RoundRecord(2, listOf("kotlin/B.kt")))),
        ) shouldBe false
        // 埋め込み値の規則は日時だけを消し、他の属性の違いは残す
        same(
            output(mapOf("java/A.java" to "@Generated(value = \"a\", date = \"d1\")\n")),
            output(mapOf("java/A.java" to "@Generated(value = \"b\", date = \"d1\")\n")),
        ) shouldBe false
        // 絶対パスの置き換えは roots に挙げたものだけ（中身に書かれた別のパスは残す）
        same(
            output(mapOf("kotlin/A.kt" to "// /opt/a\n"), roots = listOf("/tmp/r")),
            output(mapOf("kotlin/A.kt" to "// /opt/b\n"), roots = listOf("/tmp/r")),
        ) shouldBe false
    }

    "規則は冪等（2 回当てても変わらない）" {
        val out =
            output(
                mapOf("kotlin\\B.kt" to "b  \r\n", "kotlin/A.kt" to "a"),
                listOf(DiagnosticRecord(DiagnosticRecord.Severity.Error, "/r/sources/A.kt:1:1: e", null, true)),
                listOf("/r"),
            )
        val once = normalizer.normalize(out)
        normalizer.normalize(once) shouldBe once
    }

    "比較器は観点ごとに不一致を返し、読みやすい差分を付ける" {
        val k = normalizer.normalize(output(mapOf("kotlin/A.kt" to "line1\nline2\nline3\n")))
        val b = normalizer.normalize(output(mapOf("kotlin/A.kt" to "line1\nLINE2\nline3\n")))
        val mismatches = OutputComparator.compare(k, b, setOf(Aspect.GeneratedSources))
        mismatches.single().aspect shouldBe Aspect.GeneratedSources
        mismatches
            .single()
            .detail
            .lines()
            .any { it.startsWith("> 2: line2") } shouldBe true
        OutputComparator.compare(k, b, setOf(Aspect.GeneratedResources)).shouldBeEmpty()
        OutputComparator.compare(k, b, setOf(Aspect.GeneratedSources, Aspect.BuildResult)).shouldNotBeEmpty()
        k shouldNotBe b
    }
})

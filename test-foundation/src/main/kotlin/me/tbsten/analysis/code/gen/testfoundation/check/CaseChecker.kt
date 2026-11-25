package me.tbsten.analysis.code.gen.testfoundation.check

import me.tbsten.analysis.code.gen.testfoundation.CaseDeclaration
import me.tbsten.analysis.code.gen.testfoundation.Milestone

/**
 * テストケース一覧（quality/test-cases.csv）とテスト中の宣言を突き合わせ、ケースの状態を出す。
 *
 * 失敗にするもの（ADR-030）:
 * 1. 一覧に無い ID をテストが使っている
 * 2. テストの種別（基底クラス）が一覧の種別に含まれない
 * 3. 保留のマイルストーンが一覧の M 列（最初のマイルストーン）と違う
 * 4. 基準（main）で有効だったケースが、保留に戻った・テストごと消えた（例外ファイルに ADR があれば許す）
 * 5. `requireAllCases` のとき、一覧にあるのにテストが 1 本も無いケースがある
 * 6. spec を生成できない・@Ignored が付いているなど、集める段階の問題
 */
object CaseChecker {
    enum class Status { Enabled, Pending, Missing }

    data class InventoryCase(val id: String, val kind: String, val milestone: String)

    data class StatusRow(
        val id: String,
        val kind: String,
        val milestone: String,
        val status: Status,
        val reason: String,
        val tests: Int,
        val specs: String,
    )

    data class Input(
        val inventory: List<InventoryCase>,
        val declarations: List<CaseDeclaration>,
        val collectProblems: List<String> = emptyList(),
        /** 基準ブランチの状態ファイル（id → status）。null なら逆戻りの検査を飛ばす。 */
        val baseStatus: Map<String, Status>? = null,
        /** 例外ファイル（id → ADR 番号の文字列）。 */
        val exceptions: Map<String, String> = emptyMap(),
        val requireAllCases: Boolean = false,
    )

    data class Report(val rows: List<StatusRow>, val errors: List<String>, val notices: List<String>) {
        fun count(s: Status): Int = rows.count { it.status == s }
    }

    fun check(input: Input): Report {
        val errors = input.collectProblems.toMutableList()
        val notices = mutableListOf<String>()
        val inventoryById = input.inventory.associateBy { it.id }
        val declsById = input.declarations.groupBy { it.id }

        for ((id, decls) in declsById) {
            val inv = inventoryById[id]
            val where = decls.joinToString { "${it.specClass} \"${it.title}\"" }
            if (inv == null) {
                errors += "一覧に無い ID: $id（$where）。quality/test-case-inventory.md に足して gen_csv.py を実行する"
                continue
            }
            val allowedKinds = inv.kind.filter { it.isLetter() }.toSet()
            for (d in decls) {
                if (d.kind.letter !in allowedKinds) {
                    errors += "$id: 基底クラスの種別 ${d.kind.letter}（${d.specClass}）が一覧の種別「${inv.kind}」に含まれない"
                }
                val p = d.pending ?: continue
                val expected = Milestone.parseFirst(inv.milestone)
                if (expected != null && p.milestone != expected) {
                    errors += "$id: 保留のマイルストーン ${p.milestone.label} が一覧の M 列「${inv.milestone}」（${expected.label}）と違う（${d.specClass}）"
                }
            }
        }

        val rows =
            input.inventory.map { inv ->
                val decls = declsById[inv.id].orEmpty()
                val firstPending = decls.firstOrNull { it.pending != null }?.pending
                val status =
                    when {
                        decls.isEmpty() -> Status.Missing
                        firstPending != null -> Status.Pending
                        else -> Status.Enabled
                    }
                StatusRow(
                    id = inv.id,
                    kind = inv.kind,
                    milestone = inv.milestone,
                    status = status,
                    reason = firstPending?.reason?.code.orEmpty(),
                    tests = decls.size,
                    specs =
                        decls
                            .map { it.specClass }
                            .distinct()
                            .sorted()
                            .joinToString(" "),
                )
            }

        val missing = rows.filter { it.status == Status.Missing }
        if (missing.isNotEmpty()) {
            val msg = "テストが無いケース ${missing.size} 件（例: ${missing.take(5).joinToString { it.id }}）"
            if (input.requireAllCases) errors += "$msg。全ケースに保留テストが要る（acg.caseCheck.requireAllCases=true）" else notices += msg
        }

        val base = input.baseStatus
        if (base == null) {
            notices += "基準の状態ファイルが無いため、保留への逆戻りは検査していない"
        } else {
            val current = rows.associateBy { it.id }
            for ((id, was) in base) {
                if (was != Status.Enabled) continue
                val now = current[id]?.status
                if (now == Status.Enabled) continue
                val adr = input.exceptions[id]
                val what = if (now == Status.Pending) "保留に戻っている" else "テストまたは一覧から消えている"
                if (adr != null) {
                    notices += "$id: 有効だったケースが$what（例外: $adr）"
                } else {
                    errors += "$id: 基準で有効だったケースが$what。保留→有効の一方向だけ許す（R-01）。どうしても必要なら ADR を書き quality/case-exceptions.csv に足す"
                }
            }
        }
        return Report(rows, errors, notices)
    }

    // --- 状態ファイル quality/case-status.csv ---

    private val STATUS_HEADER = listOf("id", "kind", "milestone", "status", "reason", "tests", "specs")

    fun formatStatus(rows: List<StatusRow>): String =
        Csv.format(
            STATUS_HEADER,
            rows.map { listOf(it.id, it.kind, it.milestone, it.status.name.lowercase(), it.reason, it.tests.toString(), it.specs) },
        )

    fun parseStatus(text: String): Map<String, Status> =
        Csv.parseWithHeader(text).associate { r ->
            r.getValue("id") to Status.valueOf(r.getValue("status").replaceFirstChar { it.uppercase() })
        }

    fun parseInventory(text: String): List<InventoryCase> =
        Csv.parseWithHeader(text).map { InventoryCase(it.getValue("id"), it.getValue("kind"), it.getValue("milestone")) }

    fun parseExceptions(text: String): Map<String, String> =
        Csv.parseWithHeader(text).associate { r ->
            val adr = r.getValue("adr")
            require(adr.matches(Regex("ADR-\\d+"))) { "case-exceptions.csv の adr 列は ADR-n の形: '$adr'（${r["id"]}）" }
            r.getValue("id") to adr
        }
}

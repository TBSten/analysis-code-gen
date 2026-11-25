package me.tbsten.analysiscodegen.testfoundation.check

import me.tbsten.analysiscodegen.testfoundation.CaseDeclaration
import me.tbsten.analysiscodegen.testfoundation.CaseKind
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import me.tbsten.analysiscodegen.testfoundation.PendingReason

/**
 * モジュールごとの宣言一覧（`build/acg/cases.csv`）。collect が書き、check が読む。
 * 列: id, kind, pendingMilestone, pendingReason, spec, title
 */
object CaseManifest {
    private val HEADER = listOf("id", "kind", "pendingMilestone", "pendingReason", "spec", "title")

    fun write(
        declarations: List<CaseDeclaration>,
        problems: List<String>,
    ): String =
        Csv.format(
            HEADER,
            declarations.map {
                listOf(
                    it.id,
                    it.kind.letter.toString(),
                    it.pending
                        ?.milestone
                        ?.name
                        .orEmpty(),
                    it.pending
                        ?.reason
                        ?.code
                        .orEmpty(),
                    it.specClass,
                    it.title,
                )
            } + problems.map { listOf("!problem", "", "", "", "", it) },
        )

    data class Parsed(val declarations: List<CaseDeclaration>, val problems: List<String>)

    fun read(text: String): Parsed {
        val decls = mutableListOf<CaseDeclaration>()
        val problems = mutableListOf<String>()
        for (r in Csv.parseWithHeader(text)) {
            val id = r.getValue("id")
            if (id == "!problem") {
                problems += r.getValue("title")
                continue
            }
            val milestone = r.getValue("pendingMilestone")
            val pending =
                if (milestone.isEmpty()) {
                    null
                } else {
                    PendingCase(
                        Milestone.valueOf(milestone),
                        PendingReason.parse(r.getValue("pendingReason")) ?: PendingReason.NotImplemented,
                    )
                }
            decls += CaseDeclaration(id, r.getValue("title"), CaseKind.ofLetter(r.getValue("kind").single()), pending, r.getValue("spec"))
        }
        return Parsed(decls, problems)
    }
}

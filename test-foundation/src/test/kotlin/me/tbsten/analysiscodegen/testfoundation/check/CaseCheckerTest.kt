package me.tbsten.analysiscodegen.testfoundation.check

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysiscodegen.testfoundation.CaseDeclaration
import me.tbsten.analysiscodegen.testfoundation.CaseKind
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import me.tbsten.analysiscodegen.testfoundation.PendingReason
import me.tbsten.analysiscodegen.testfoundation.check.CaseChecker.Status

class CaseCheckerTest : FreeSpec({
    val inventory =
        listOf(
            CaseChecker.InventoryCase("GRD-01", "E", "M1"),
            CaseChecker.InventoryCase("INC-14", "E", "M2"),
            CaseChecker.InventoryCase("BRG-07", "E / D", "M2 / M3"),
            CaseChecker.InventoryCase("PBT-01", "P", "任意"),
        )

    fun decl(
        id: String,
        kind: CaseKind = CaseKind.E2E,
        pending: PendingCase? = null,
        spec: String = "a.Spec",
    ) = CaseDeclaration(id, "t-$id", kind, pending, spec)

    fun check(
        decls: List<CaseDeclaration>,
        base: Map<String, Status>? = null,
        exceptions: Map<String, String> = emptyMap(),
        requireAll: Boolean = false,
    ) = CaseChecker.check(CaseChecker.Input(inventory, decls, emptyList(), base, exceptions, requireAll))

    "一覧どおりの宣言ならエラーなしで状態が出る" {
        val r =
            check(
                listOf(
                    decl("GRD-01"),
                    decl("INC-14", pending = PendingCase(Milestone.M2)),
                    decl("BRG-07", kind = CaseKind.Diff, pending = PendingCase(Milestone.M2, PendingReason.Adr(30))),
                ),
            )
        r.errors.shouldBeEmpty()
        r.rows.associate { it.id to it.status } shouldBe
            mapOf("GRD-01" to Status.Enabled, "INC-14" to Status.Pending, "BRG-07" to Status.Pending, "PBT-01" to Status.Missing)
        r.rows.single { it.id == "BRG-07" }.reason shouldBe "ADR-030"
        r.notices.joinToString() shouldContain "テストが無いケース 1 件"
    }

    "一覧に無い ID はエラー" {
        check(listOf(decl("XXX-01"))).errors.single() shouldContain "一覧に無い ID: XXX-01"
    }

    "種別が一覧に含まれないとエラー" {
        check(listOf(decl("GRD-01", kind = CaseKind.Unit))).errors.single() shouldContain "種別 U"
    }

    "保留のマイルストーンが一覧と違うとエラー（M 列は最初のマイルストーンで比べる）" {
        check(listOf(decl("INC-14", pending = PendingCase(Milestone.M1)))).errors.single() shouldContain "M1"
        check(listOf(decl("BRG-07", pending = PendingCase(Milestone.M2)))).errors.shouldBeEmpty()
        check(listOf(decl("PBT-01", kind = CaseKind.Property, pending = PendingCase(Milestone.Optional)))).errors.shouldBeEmpty()
    }

    "requireAllCases ならテストの無いケースはエラー" {
        check(listOf(decl("GRD-01")), requireAll = true).errors.single() shouldContain "テストが無いケース 3 件"
    }

    "同じ ID のテストが 1 本でも保留ならケースは保留" {
        val r = check(listOf(decl("GRD-01"), decl("GRD-01", pending = PendingCase(Milestone.M1), spec = "b.Spec")))
        r.rows.single { it.id == "GRD-01" }.let {
            it.status shouldBe Status.Pending
            it.tests shouldBe 2
            it.specs shouldBe "a.Spec b.Spec"
        }
    }

    "基準で有効だったケースが保留に戻ったらエラー" {
        val r = check(listOf(decl("GRD-01", pending = PendingCase(Milestone.M1))), base = mapOf("GRD-01" to Status.Enabled))
        r.errors.single() shouldContain "保留に戻っている"
    }

    "基準で有効だったケースが消えたらエラー" {
        check(emptyList(), base = mapOf("GRD-01" to Status.Enabled)).errors.single() shouldContain "消えている"
        check(emptyList(), base = mapOf("GONE-01" to Status.Enabled)).errors shouldHaveSize 1
    }

    "例外ファイルに ADR があれば逆戻りを許す" {
        val r =
            check(
                listOf(decl("GRD-01", pending = PendingCase(Milestone.M1))),
                base = mapOf("GRD-01" to Status.Enabled),
                exceptions = mapOf("GRD-01" to "ADR-040"),
            )
        r.errors.shouldBeEmpty()
        r.notices.joinToString() shouldContain "ADR-040"
    }

    "保留から有効・保留のままは問題なし" {
        check(listOf(decl("GRD-01")), base = mapOf("GRD-01" to Status.Pending, "INC-14" to Status.Missing)).errors.shouldBeEmpty()
    }

    "集める段階の問題はそのままエラー" {
        CaseChecker.check(CaseChecker.Input(inventory, emptyList(), listOf("x: 生成できない"))).errors shouldBe listOf("x: 生成できない")
    }

    "状態ファイルは書いて読み戻せる" {
        val r = check(listOf(decl("GRD-01"), decl("INC-14", pending = PendingCase(Milestone.M2))))
        val text = CaseChecker.formatStatus(r.rows)
        text.lines().first() shouldBe "id,kind,milestone,status,reason,tests,specs"
        CaseChecker.parseStatus(text) shouldBe r.rows.associate { it.id to it.status }
    }
})

package me.tbsten.analysis.code.gen.testfoundation.check

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import me.tbsten.analysis.code.gen.testfoundation.CaseDeclaration
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import java.io.File

// リポジトリの quality/test-cases.csv を実際に読み、DSL と整合チェックが前提にする書式から外れていないかを見る
class InventoryTest : FreeSpec({
    val csv = File("../quality/test-cases.csv")
    val inventory = CaseChecker.parseInventory(csv.readText())

    "一覧が読める" {
        inventory.size shouldBeGreaterThan 200
    }

    "全 ID が DSL の ID 書式に合う" {
        inventory.filterNot { CaseDeclaration.ID_PATTERN.matches(it.id) }.shouldBeEmpty()
    }

    "全ケースの M 列から最初のマイルストーンが読める" {
        inventory.filter { Milestone.parseFirst(it.milestone) == null }.shouldBeEmpty()
    }

    "全ケースの種別が U / I / E / D / P / B の組み合わせ" {
        inventory
            .filterNot { inv ->
                inv.kind.filter { it.isLetter() }.let { it.isNotEmpty() && it.all { c -> c in "UIEDPB" } }
            }.shouldBeEmpty()
    }

    "CSV の引用（カンマ・改行・二重引用符）を読める" {
        Csv.parse("a,\"b,c\",\"d\"\"e\"\n1,\"x\ny\",z\n") shouldBe listOf(listOf("a", "b,c", "d\"e"), listOf("1", "x\ny", "z"))
    }
})

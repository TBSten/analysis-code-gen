package me.tbsten.analysis.code.gen.testfoundation.check

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysis.code.gen.testfoundation.sampleAbstractSpecClass
import me.tbsten.analysis.code.gen.testfoundation.sampleBrokenSpecClass
import me.tbsten.analysis.code.gen.testfoundation.sampleE2ESpecClass
import me.tbsten.analysis.code.gen.testfoundation.sampleIgnoredSpecClass
import me.tbsten.analysis.code.gen.testfoundation.sampleUnitSpecClass

class CaseCollectorTest : FreeSpec({
    "AcgSpec のサブクラスだけを生成して宣言を集める（抽象クラス・無関係なクラスは飛ばす）" {
        val r = CaseCollector.collect(listOf(sampleE2ESpecClass, sampleUnitSpecClass, sampleAbstractSpecClass, String::class.java))
        r.problems.shouldBeEmpty()
        r.declarations.map { it.id } shouldContainExactly listOf("GRD-01", "GRD-02", "GRD-03", "INC-01a")
    }

    "@Ignored 付きの AcgSpec は問題として報告する" {
        CaseCollector.collect(listOf(sampleIgnoredSpecClass)).problems.single() shouldContain "@Ignored"
    }

    "生成に失敗した spec は問題として報告する" {
        val r = CaseCollector.collect(listOf(sampleBrokenSpecClass))
        r.declarations.shouldBeEmpty()
        r.problems.single() shouldContain "grd-1"
    }

    "manifest は書いて読み戻せる" {
        val r = CaseCollector.collect(listOf(sampleE2ESpecClass))
        val parsed = CaseManifest.read(CaseManifest.write(r.declarations, listOf("問題, \"引用\" 付き")))
        parsed.declarations shouldBe r.declarations
        parsed.problems shouldBe listOf("問題, \"引用\" 付き")
    }
})

package me.tbsten.analysis.code.gen.pbt

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import me.tbsten.analysis.code.gen.pbt.change.ChangeHistoryShrinker
import me.tbsten.analysis.code.gen.pbt.change.changeHistory
import me.tbsten.analysis.code.gen.pbt.compile.JvmCompileCheck
import me.tbsten.analysis.code.gen.pbt.kmp.KmpHierarchyShrinker
import me.tbsten.analysis.code.gen.pbt.kmp.kmpHierarchy
import me.tbsten.analysis.code.gen.pbt.processor.OptionTable
import me.tbsten.analysis.code.gen.pbt.processor.Pairwise
import me.tbsten.analysis.code.gen.pbt.processor.ProcessorId
import me.tbsten.analysis.code.gen.pbt.processor.ProcessorSet
import me.tbsten.analysis.code.gen.pbt.processor.ProcessorSetShrinker
import me.tbsten.analysis.code.gen.pbt.processor.options
import me.tbsten.analysis.code.gen.pbt.processor.processorSet
import me.tbsten.analysis.code.gen.pbt.processor.processorSets
import me.tbsten.analysis.code.gen.pbt.source.isValid
import me.tbsten.analysis.code.gen.pbt.source.render

private val config = PropTestConfig(seed = 4040L, iterations = 200)

class GeneratorsTest : FreeSpec({
    "Processor の組: KMP に置けない Processor を KMP に置かない・縮小しても妥当" {
        checkAll(config, Arb.processorSet()) { set ->
            ProcessorSet.allowed(set.order, set.placement) shouldBe true
            ProcessorSetShrinker.shrink(set).forEach { ProcessorSet.allowed(it.order, it.placement) shouldBe true }
        }
    }

    "Processor の組の網羅層は単体 6 + 順序付きペア 30 = 36 通り" {
        val all = Exhaustive.processorSets().values
        all.size shouldBe 36
        all.toSet().size shouldBe 36
        all.filter { it.order.size == 2 }.map { it.order } shouldContainAll
            listOf(listOf(ProcessorId.KOI, ProcessorId.CRM), listOf(ProcessorId.CRM, ProcessorId.KOI))
    }

    "オプション: 表にある名前と値だけを出す" {
        checkAll(config, Arb.processorSet()) { set ->
            val specs = set.order.flatMap { OptionTable.of(it) }.associateBy { it.name }
            Arb
                .options(set)
                .sample(
                    io.kotest.property.RandomSource
                        .seeded(set.hashCode().toLong()),
                ).value.values
                .forEach { (k, v) ->
                    (v in specs.getValue(k).values) shouldBe true
                }
        }
    }

    "全ペア網羅: どの 2 パラメータの値の組も 1 行以上に現れ、同じ入力には同じ結果を返す" {
        checkAll(PropTestConfig(seed = 1L, iterations = 100), Arb.list(Arb.int(1..5), 0..7)) { sizes ->
            val rows = Pairwise.cover(sizes)
            for (i in sizes.indices) {
                for (j in i + 1 until sizes.size) {
                    for (a in 0 until sizes[i]) {
                        for (b in 0 until sizes[j]) {
                            withClue("$sizes ($i=$a, $j=$b)") { rows.any { it[i] == a && it[j] == b } shouldBe true }
                        }
                    }
                }
            }
            Pairwise.cover(sizes).map { it.toList() } shouldBe rows.map { it.toList() }
        }
    }

    "全ペア網羅はオプション全体の直積よりずっと少ない（Koin + cream.kt）" {
        val set = ProcessorSet(listOf(ProcessorId.KOI, ProcessorId.CRM))
        val product = set.order.flatMap { OptionTable.of(it) }.fold(1L) { acc, s -> acc * s.values.size }
        val rows = Pairwise.options(set)
        (rows.size < product / 10) shouldBe true
    }

    "KMP 階層: 根は commonMain・各ターゲットに葉が 1 つ・縮小しても妥当" {
        checkAll(config, Arb.kmpHierarchy()) { h ->
            h.problems().shouldBeEmpty()
            h.targets.forEach { t -> h.targetsOf(t.sourceSet) shouldBe listOf(t) }
            h.targetsOf("commonMain").toSet() shouldBe h.targets.toSet()
            KmpHierarchyShrinker.shrink(h).forEach { it.problems().shouldBeEmpty() }
        }
    }

    "変更の列: どの状態も妥当（コンパイルできる形）で、縮小しても適用できる" {
        checkAll(PropTestConfig(seed = 7L, iterations = 60), Arb.changeHistory()) { history ->
            val states = history.states()
            states.size shouldBe history.ops.size + 1
            states.forEach { s -> withClue(s.project) { s.project.isValid() shouldBe true } }
            ChangeHistoryShrinker.shrink(history).take(5).forEach {
                it
                    .states()
                    .last()
                    .project
                    .isValid() shouldBe true
            }
        }
    }

    "変更の列: 最後の状態は実際にコンパイルできる" {
        checkAll(PropTestConfig(seed = 8L, iterations = 10), Arb.changeHistory()) { history ->
            val last = history.states().last().project
            val result = JvmCompileCheck.compile(render(last))
            withClue(result.log) { result.ok shouldBe true }
        }
    }
})

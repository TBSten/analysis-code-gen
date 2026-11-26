package me.tbsten.analysis.code.gen.e2e.xd

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import me.tbsten.analysis.code.gen.e2e.support.CoverageLabels
import me.tbsten.analysis.code.gen.e2e.support.catalog
import me.tbsten.analysis.code.gen.e2e.support.loadRealKspProvider
import me.tbsten.analysis.code.gen.e2e.support.repoRoot
import me.tbsten.analysis.code.gen.pbt.change.BuildState
import me.tbsten.analysis.code.gen.pbt.change.ChangeOp
import me.tbsten.analysis.code.gen.pbt.change.changeHistory
import me.tbsten.analysis.code.gen.pbt.processor.OptionTable
import me.tbsten.analysis.code.gen.pbt.processor.Pairwise
import me.tbsten.analysis.code.gen.pbt.processor.ProcessorId
import me.tbsten.analysis.code.gen.pbt.processor.ProcessorSet
import me.tbsten.analysis.code.gen.pbt.run.acgCheckAll
import me.tbsten.analysis.code.gen.pbt.source.render
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PbtSpec
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import me.tbsten.analysis.code.gen.testfoundation.UnitSpec
import me.tbsten.analysis.code.gen.testing.CodegenTestSpec
import me.tbsten.analysis.code.gen.testing.TestEngine
import me.tbsten.analysis.code.gen.testing.runCodegenScenario
import me.tbsten.analysis.code.gen.testing.runCodegenTest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// XD-09（状態機械 PBT）と XD-11（24 時間の網羅率）。決まりは ADR-037

/** 状態のソース（単一モジュール、SourceSet のソースルートからの相対パス → 中身）。 */
private fun BuildState.sources(): Map<String, String> =
    render(project)
        .modules
        .first()
        .files
        .mapKeys { (p, _) -> p.substringAfter("src/main/kotlin/").substringAfter("src/main/java/") }

private fun CodegenTestSpec.load(state: BuildState) {
    state.sources().forEach { (path, content) -> if (path.endsWith(".java")) java(path, content) else kotlin(path, content) }
    state.processors.order.forEach { ksp(loadRealKspProvider(it)) }
    options.putAll(state.options.values)
}

class XdIncrementalPbtTest : PbtSpec({
    case(
        "XD-09",
        "変更の列（状態機械 PBT）の各ステップで、B の増分結果 = B の最終状態のクリーンビルド = K のクリーンビルド",
        pending = PendingCase(Milestone.M2),
    ) {
        // testing のシナリオは途中で Processor を差し替えられないので、Processor の追加・削除を含む列は Gradle の INC 系で見る（ADR-037）
        val histories = Arb.changeHistory().filter { h -> h.ops.none { it is ChangeOp.ToggleProcessor } }
        acgCheckAll("XD-09 B の増分 = B のクリーン = K のクリーン", histories, weight = 2.0) { history ->
            val states = history.states()
            val incremental =
                runCodegenScenario(TestEngine.AnalysisCodegen) {
                    initial { load(states.first()) }
                    states.zipWithNext().forEachIndexed { i, (before, after) ->
                        step("${i + 1}: ${history.ops[i]}") {
                            val old = before.sources()
                            val new = after.sources()
                            (old.keys - new.keys).forEach { delete(it) }
                            new.filter { (p, c) -> old[p] != c }.forEach { (p, c) -> if (p.endsWith(".java")) java(p, c) else kotlin(p, c) }
                            (before.options.values.keys + after.options.values.keys).forEach { k -> option(k, after.options.values[k]) }
                        }
                    }
                }
            incremental.size shouldBe states.size
            incremental.zip(states).forEach { (step, state) ->
                val bClean = runCodegenTest(TestEngine.AnalysisCodegen) { load(state) }
                val kClean = runCodegenTest(TestEngine.Ksp2) { load(state) }
                withClue("${step.stepName}\n$state") {
                    step.result.isSuccess shouldBe kClean.isSuccess
                    step.result.generatedFiles shouldBe bClean.generatedFiles
                    bClean.generatedFiles shouldBe kClean.generatedFiles
                }
            }
        }
    }
})

class XdCoverageTest : UnitSpec({
    case(
        "XD-11",
        "Kotlin の版ごとに、その版の一巡（版の数 × 24 時間）の定期実行で、カタログのパターン・オプション値・オプション値の組・Processor のペアの網羅率がすべて 100%",
        pending = PendingCase(Milestone.M2),
    ) {
        // 毎時のワークフロー（pbt-hourly.yml）は acgCheckAll / acgCheckExhaustive が書いた網羅の記録（build/pbt/coverage/*.tsv）を
        // .pbt-state/coverage/<Kotlin の版>/<UTC の yyyyMMddHH>/ に写して持ち回る。網羅率は版ごとに独立に数える（ADR-055）。
        // 区画と版の割り当てを 1 日ずつずらすので、版の数 × 24 時間で「全版 × 全区画」が一巡する。
        // 1 行の先頭の欄は空白区切りで複数のラベルを持てる（XD-04・06・07 の網羅層の ID）
        val versions =
            repoRoot
                .resolve("gradle/kotlin-versions.txt")
                .readLines()
                .map { it.substringBefore('#').trim() }
                .filter { it.isNotEmpty() }
                .map { it.split(Regex("\\s+")).first() }
        val hour = DateTimeFormatter.ofPattern("yyyyMMddHH").withZone(ZoneOffset.UTC)
        val window = (0 until versions.size * 24).map { hour.format(Instant.now().minusSeconds(3600L * it)) }.toSet()

        fun seen(version: String): Set<String> =
            repoRoot
                .resolve(".pbt-state/coverage/$version")
                .listFiles()
                .orEmpty()
                .filter { it.isDirectory && it.name in window }
                .flatMap { dir -> dir.listFiles().orEmpty().filter { it.extension == "tsv" } }
                .flatMap { f -> f.readLines().filter { it.isNotBlank() }.flatMap { it.substringBefore('\t').split(' ') } }
                .toSet()

        val expected =
            buildSet {
                catalog.usable.forEach { add(CoverageLabels.pattern(it.id)) }
                ProcessorId.entries.forEach { id ->
                    val specs = OptionTable.of(id)
                    specs.forEach { s -> s.values.forEach { v -> add(CoverageLabels.option(s.name, v)) } }
                    for (i in specs.indices) {
                        for (j in i + 1 until specs.size) {
                            for (a in specs[i].values) {
                                for (b in specs[j].values) {
                                    add(
                                        CoverageLabels.optionPair(
                                            specs[i].name to a,
                                            specs[j].name to b,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
                ProcessorSet.singlesAndOrderedPairs().filter { it.order.size == 2 }.forEach { add(CoverageLabels.processorPair(it)) }
            }
        // 全ペアの表が本当にすべての組を覆う形になっている（網羅の基準そのものの確認）
        ProcessorId.entries.forEach { id -> Pairwise.cover(OptionTable.of(id).map { it.values.size }).isNotEmpty() shouldBe true }

        val missing = versions.associateWith { (expected - seen(it)).sorted() }.filterValues { it.isNotEmpty() }
        val report = missing.entries.joinToString("\n") { (v, m) -> "$v: ${m.size} 件（先頭 10 件）\n" + m.take(10).joinToString("\n") }
        withClue("その版の一巡で一度も出ていないもの:\n$report") {
            missing.keys.shouldBeEmpty()
        }
    }
})

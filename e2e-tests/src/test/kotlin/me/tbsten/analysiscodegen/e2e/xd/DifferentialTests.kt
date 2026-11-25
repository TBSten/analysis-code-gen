package me.tbsten.analysiscodegen.e2e.xd

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.map
import me.tbsten.analysiscodegen.differential.Aspect
import me.tbsten.analysiscodegen.differential.ComparisonPolicy
import me.tbsten.analysiscodegen.differential.DiffInput
import me.tbsten.analysiscodegen.differential.DiffMilestone
import me.tbsten.analysiscodegen.differential.DiffResult
import me.tbsten.analysiscodegen.differential.DiffSource
import me.tbsten.analysiscodegen.differential.EngineKind
import me.tbsten.analysiscodegen.differential.EngineRun
import me.tbsten.analysiscodegen.differential.Expectation
import me.tbsten.analysiscodegen.e2e.support.CoverageLabels
import me.tbsten.analysiscodegen.e2e.support.catalog
import me.tbsten.analysiscodegen.e2e.support.harness
import me.tbsten.analysiscodegen.e2e.support.requiresNative
import me.tbsten.analysiscodegen.e2e.support.toDiffInput
import me.tbsten.analysiscodegen.pbt.catalog.CatalogPattern
import me.tbsten.analysiscodegen.pbt.catalog.fits
import me.tbsten.analysiscodegen.pbt.catalog.userProject
import me.tbsten.analysiscodegen.pbt.catalog.userProjectEdgecases
import me.tbsten.analysiscodegen.pbt.processor.OptionTable
import me.tbsten.analysiscodegen.pbt.processor.Pairwise
import me.tbsten.analysiscodegen.pbt.processor.Placement
import me.tbsten.analysiscodegen.pbt.processor.ProcessorId
import me.tbsten.analysiscodegen.pbt.processor.ProcessorSet
import me.tbsten.analysiscodegen.pbt.processor.processorSet
import me.tbsten.analysiscodegen.pbt.run.acgCheckAll
import me.tbsten.analysiscodegen.pbt.run.acgCheckExhaustive
import me.tbsten.analysiscodegen.testfoundation.DiffSpec
import me.tbsten.analysiscodegen.testfoundation.E2ESpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase

// 5.29 三者差分テスト（XD）。ハーネスは :differential（ADR-023）、入力はカタログ（ADR-020）と :pbt の Arb（ADR-022）
// XD-01 は ACG-40、XD-03・16・17 は ACG-42 が書いた。XD-05・10 は Native の Provider の型に触れるので :engine-tests、
// XD-09 は XdIncrementalPbtTest、XD-11 は XdCoverageTest
// 比べる強さは ComparisonPolicy.forBridge / forNative（必須の観点を独自に減らさない）

/** 単体の Processor と、それに合うパターン（背景コード以外）の組すべて。 */
private fun singleProcessorInputs(): List<Pair<ProcessorSet, CatalogPattern>> =
    ProcessorId.entries.flatMap { id ->
        val set = ProcessorSet(listOf(id))
        catalog.usable.filter { !it.background && it.fits(set) }.map { set to it }
    }

/** B（と、組に必須の移植版があれば N）で比べ、必須の観点が一致する。比べられなかったら落とす。 */
private fun DiffResult.shouldMatch(
    set: ProcessorSet,
    requireNative: Boolean = set.requiresNative(),
) {
    withClue(report()) {
        runs.getValue(EngineKind.K).shouldBeInstanceOf<EngineRun.Completed>()
        requireCompared(EngineKind.B)
        if (requireNative) requireCompared(EngineKind.N)
        assertRequiredMatch()
    }
}

class DifferentialE2ETest : E2ESpec({
    case(
        "XD-02",
        "Arb が作った有効な入力は K でビルドが通り、わざと誤った入力は K で想定どおりに失敗する（入力の分類が正しい）",
        pending = PendingCase(Milestone.M1),
    ) {
        val inputs =
            Arb
                .processorSet(max = 2)
                .filter { set -> catalog.verified.any { !it.background && it.fits(set) } && !set.placement.isKmp }
                .flatMap { set -> Arb.userProject(catalog, set).map { set to it } }
        val k = harness()
        acgCheckAll("XD-02 入力の分類が K の結果と合う", inputs, labels = { (set, p) ->
            p.ids.map(CoverageLabels::pattern) +
                CoverageLabels.processorPair(set)
        }) { (set, project) ->
            val input = project.toDiffInput(set)
            val run = k.runEngine(EngineKind.K, input)
            withClue("$project @ $set") {
                run.shouldBeInstanceOf<EngineRun.Completed>()
                when (input.expectation) {
                    Expectation.Valid -> run.output.success shouldBe true
                    Expectation.Invalid -> {
                        run.output.success shouldBe false
                        // わざと誤った入力は、カタログに書いた理由で失敗する（別の理由で落ちて「想定どおり」に見えるのを防ぐ）
                        project.patterns.filter { it.isError && it.expectMessage.isNotBlank() }.forEach { p ->
                            run.output.diagnostics.joinToString("\n") { it.message } shouldContain p.expectMessage
                        }
                    }
                    Expectation.Unknown -> error("カタログの入力は Valid か Invalid のどちらか")
                }
            }
        }
    }
})

class DifferentialDiffTest : DiffSpec({
    case(
        "XD-04",
        "Processor 単体 × カタログの全パターンで、K と B の生成コード・リソースが正規化後に一致する",
        pending = PendingCase(Milestone.M2),
    ) {
        val h = harness(bridge = ComparisonPolicy.forBridge(DiffMilestone.M2))
        acgCheckExhaustive("XD-04 単体 × 全パターンで K と B が一致", singleProcessorInputs(), id = { (set, p) -> coverageId(set, p) }) { (set, p) ->
            h.run(p.toDiffInput(set)).shouldMatch(set, requireNative = false)
        }
    }

    case(
        "XD-06",
        "Processor 2 個の全ペア（順番の入れ替えを含む）で、K / B / N の出力が XD-04・XD-05 と同じ基準で一致する",
        pending = PendingCase(Milestone.M2),
    ) {
        val h = harness(ComparisonPolicy.forBridge(DiffMilestone.M2), ComparisonPolicy.forNative(DiffMilestone.M2))
        // 30 通りの順序付きペア × そのペアに合うパターン 1 つずつ（userProjectEdgecases）
        val inputs =
            ProcessorSet.singlesAndOrderedPairs().filter { it.order.size == 2 }.flatMap { set ->
                userProjectEdgecases(set, catalog.usable).map { set to it }
            }
        inputs.map { it.first }.distinct().size shouldBe 30
        acgCheckExhaustive("XD-06 全ペアで K / B / N が一致", inputs, id = { (set, p) ->
            (
                p.ids.map(CoverageLabels::pattern) +
                    CoverageLabels.processorPair(set)
            ).joinToString(" ")
        }) { (set, project) ->
            h.run(project.toDiffInput(set)).shouldMatch(set)
        }
    }

    case(
        "XD-07",
        "Processor ごとの全オプション値と、オプション値の全ペアで、K / B / N の出力が一致する",
        pending = PendingCase(Milestone.M2),
    ) {
        val h = harness()
        val inputs =
            ProcessorId.entries.flatMap { id ->
                val set = ProcessorSet(listOf(id))
                val specs = OptionTable.of(id)
                val patterns = catalog.usable.filter { !it.background && it.fits(set) }.take(3)
                // 全ペアを覆う表（Pairwise）は全値も覆う。値が 1 つのオプションしか無い Processor は既定値の 1 行
                val rows =
                    Pairwise.cover(specs.map { it.values.size }).map { row ->
                        specs.mapIndexed { i, s -> s.name to s.values[row[i]] }.toMap()
                    }
                rows.flatMap { options -> patterns.map { Triple(set, it, options) } }
            }
        acgCheckExhaustive("XD-07 全オプション値と全ペアで K / B / N が一致", inputs, id = { (set, p, o) -> coverageId(set, p, o) }) { (set, p, options) ->
            h.run(p.toDiffInput(set, options)).shouldMatch(set)
        }
    }

    case(
        "XD-08",
        "ビルドの成否とコンパイラ診断（誤った入力を含む）が K / B / N で一致する",
        pending = PendingCase(Milestone.M2),
    ) {
        val strict = ComparisonPolicy(required = setOf(Aspect.BuildResult, Aspect.Diagnostics))
        val h = harness(strict, strict)
        // わざと誤った入力をすべて + 有効な入力の網羅層
        val invalid = singleProcessorInputs().filter { (_, p) -> p.isError }
        invalid.shouldNotBeEmptyList()
        acgCheckExhaustive("XD-08 成否と診断が一致（誤った入力）", invalid, id = { (set, p) -> coverageId(set, p) }) { (set, p) ->
            val result = h.run(p.toDiffInput(set))
            result.shouldMatch(set)
            // K で本当に失敗している（誤った入力が誤りとして扱われている）
            (result.runs.getValue(EngineKind.K) as EngineRun.Completed).output.success shouldBe false
        }
        acgCheckExhaustive("XD-08 成否と診断が一致（全パターン）", singleProcessorInputs(), id = { (set, p) -> coverageId(set, p) }) { (set, p) ->
            h.run(p.toDiffInput(set)).shouldMatch(set)
        }
    }

    case(
        "XD-12",
        "K と N の生成コードが正規化後に一致する（段階 2）。差は ADR の許容リストに載ったものだけ",
        pending = PendingCase(Milestone.M3),
    ) {
        // 段階 2 の強さ（forNative(M3)）。許容リストは Normalizer の規則として持つ（ADR-023）ので、ここでは足さない
        val h = harness(native = ComparisonPolicy.forNative(DiffMilestone.M3))
        val inputs = singleProcessorInputs().filter { (set, _) -> set.requiresNative() }
        acgCheckExhaustive("XD-12 K と N の生成コードが一致（段階 2）", inputs, id = { (set, p) -> coverageId(set, p) }) { (set, p) ->
            val result = h.run(p.toDiffInput(set))
            withClue(result.report()) {
                result.requireCompared(EngineKind.N)
                result.comparison(EngineKind.N).requiredMismatches.shouldBeEmpty()
            }
        }
    }

    case("XD-13", "Logger 出力（レベル・位置・文言）が K と B で一致する", pending = PendingCase(Milestone.M3)) {
        val h = harness(bridge = ComparisonPolicy(required = setOf(Aspect.Logs)))
        acgCheckExhaustive("XD-13 Logger 出力が K と B で一致", singleProcessorInputs(), id = { (set, p) -> coverageId(set, p) }) { (set, p) ->
            val result = h.run(p.toDiffInput(set))
            withClue(result.report()) {
                result.requireCompared(EngineKind.B)
                result.comparison(EngineKind.B).requiredMismatches.shouldBeEmpty()
            }
        }
    }

    case(
        "XD-14",
        "Logger 出力のレベルと位置が K と N で一致する（文言は努力目標として計測・表示）",
        pending = PendingCase(Milestone.M3),
    ) {
        val h = harness(native = ComparisonPolicy(required = setOf(Aspect.LogLevelsAndLocations), measured = setOf(Aspect.Logs)))
        val inputs = singleProcessorInputs().filter { (set, _) -> set.requiresNative() }
        acgCheckExhaustive("XD-14 Logger のレベルと位置が K と N で一致", inputs, id = { (set, p) -> coverageId(set, p) }) { (set, p) ->
            val result = h.run(p.toDiffInput(set))
            withClue(result.report()) {
                result.requireCompared(EngineKind.N)
                result.comparison(EngineKind.N).requiredMismatches.shouldBeEmpty()
            }
        }
    }

    case("XD-15", "ラウンド数と各ラウンドの生成ファイルが K と B で一致する", pending = PendingCase(Milestone.M3)) {
        val h = harness(bridge = ComparisonPolicy(required = setOf(Aspect.Rounds)))
        acgCheckExhaustive("XD-15 ラウンドが K と B で一致", singleProcessorInputs(), id = { (set, p) -> coverageId(set, p) }) { (set, p) ->
            val result = h.run(p.toDiffInput(set))
            withClue(result.report()) {
                result.requireCompared(EngineKind.B)
                // ラウンドを測れない実行系だと比較が黙って飛ぶので、両方が測れていることを先に確かめる
                result.normalized
                    .getValue(EngineKind.K)
                    .rounds
                    .shouldNotBeNull()
                result.normalized
                    .getValue(EngineKind.B)
                    .rounds
                    .shouldNotBeNull()
                result.comparison(EngineKind.B).requiredMismatches.shouldBeEmpty()
            }
        }
    }

    case(
        "XD-18",
        "KMP の配置（commonMain / プラットフォーム）を入力の次元に加えても、K（Compatible モード）と B の出力が一致する",
        pending = PendingCase(Milestone.M3),
    ) {
        // KMP に置ける Processor のパターンを commonMain と jvmMain に置く。K は Gradle フィクスチャのランナーで動かす（ADR-023）
        val h = harness(bridge = ComparisonPolicy.forBridge(DiffMilestone.M3))
        val inputs =
            singleProcessorInputs()
                .filter { (set, _) -> set.order.all { it.kmp } }
                .flatMap { (set, p) ->
                    listOf(Placement.KMP_COMMON, Placement.KMP_PLATFORM).map { Triple(set.copy(placement = it), p, it) }
                }
        acgCheckExhaustive("XD-18 KMP の配置で K と B が一致", inputs, id = { (set, p, _) ->
            coverageId(set, p) + " placement:${set.placement}"
        }) { (set, p, placement) ->
            val base = p.toDiffInput(set)
            val sourceSet = if (placement == Placement.KMP_COMMON) "commonMain" else "jvmMain"
            val input: DiffInput = base.copy(sources = base.sources.map { DiffSource(it.path, it.content, sourceSet) })
            h.run(input).shouldMatch(set, requireNative = false)
        }
    }
})

/**
 * 網羅層の 1 件の ID。空白区切りで網羅のラベル（CoverageLabels）を並べる。acgCheckExhaustive はこの ID を網羅の記録に書くので、
 * XD-11 が空白で分けて「パターン・オプション値・オプション値の組・Processor のペア」を数えられる
 */
private fun coverageId(
    set: ProcessorSet,
    p: CatalogPattern,
    options: Map<String, String> = emptyMap(),
): String {
    val opts = options.entries.map { it.key to it.value }
    val pairs = opts.flatMapIndexed { i, a -> opts.drop(i + 1).map { b -> CoverageLabels.optionPair(a, b) } }
    return (
        listOf(CoverageLabels.pattern(p.id), CoverageLabels.processorPair(set)) +
            opts.map { CoverageLabels.option(it.first, it.second) } +
            pairs
    ).joinToString(" ")
}

private fun <T> List<T>.shouldNotBeEmptyList() = withClue("入力が空") { isEmpty() shouldBe false }

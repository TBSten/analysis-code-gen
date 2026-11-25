package me.tbsten.analysiscodegen.enginetests.xd

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import me.tbsten.analysiscodegen.pbt.catalog.CatalogPattern
import me.tbsten.analysiscodegen.pbt.catalog.PatternCatalog
import me.tbsten.analysiscodegen.pbt.catalog.fits
import me.tbsten.analysiscodegen.pbt.change.BuildState
import me.tbsten.analysiscodegen.pbt.change.ChangeOp
import me.tbsten.analysiscodegen.pbt.change.changeHistory
import me.tbsten.analysiscodegen.pbt.processor.NativePort
import me.tbsten.analysiscodegen.pbt.processor.ProcessorId
import me.tbsten.analysiscodegen.pbt.processor.ProcessorSet
import me.tbsten.analysiscodegen.pbt.run.acgCheckAll
import me.tbsten.analysiscodegen.pbt.run.acgCheckExhaustive
import me.tbsten.analysiscodegen.pbt.source.render
import me.tbsten.analysiscodegen.testfoundation.DiffSpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PbtSpec
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import me.tbsten.analysiscodegen.testing.CodegenTestResult
import me.tbsten.analysiscodegen.testing.CodegenTestSpec
import me.tbsten.analysiscodegen.testing.TestEngine
import me.tbsten.analysiscodegen.testing.runCodegenScenario
import me.tbsten.analysiscodegen.testing.runCodegenTest

// 5.29 三者差分（XD）のうち K と N を段階 1（公開 ABI と実行時テストの結果）で比べるもの（XD-05・XD-10）。
// N の Processor を渡すのに analysis-code-gen-processor-api の型が要るので :engine-tests に置く（ADR-037）。他の XD は e2e-tests の e2e.xd

private val catalog: PatternCatalog by lazy { PatternCatalog.loadOrNull() ?: error("quality/catalog/index.json が読めない") }

/** 合格ラインに入る移植版（REQUIRED）を持つ Processor。 */
private val requiredNative: List<ProcessorId> = ProcessorId.entries.filter { it.native == NativePort.REQUIRED }

/** K と N を段階 1 で比べる: 成否が同じで、成功なら公開 ABI と実行時テストの結果が同じ。 */
private fun stage1ShouldMatch(
    k: CodegenTestResult,
    n: CodegenTestResult,
    sources: Map<String, String>,
) {
    withClue("K:\n${k.messages}\nN:\n${n.messages}") {
        n.isSuccess shouldBe k.isSuccess
        if (k.isSuccess) {
            n.publicAbi() shouldBe k.publicAbi()
            n.runtimeCheck(sources) shouldBe k.runtimeCheck(sources)
        }
    }
}

private fun CodegenTestSpec.processorsFor(
    engine: TestEngine,
    ids: List<ProcessorId>,
) = ids.forEach { id ->
    when (engine) {
        TestEngine.Ksp2 -> ksp(realKspProvider(id))
        TestEngine.AnalysisCodegen -> native(nativePortProvider(id))
    }
}

private fun runStage(
    engine: TestEngine,
    sources: Map<String, String>,
    ids: List<ProcessorId>,
    options: Map<String, String>,
): CodegenTestResult =
    runCodegenTest(engine) {
        loadSources(sources, ids)
        processorsFor(engine, ids)
        this.options.putAll(options)
    }

/** XD-05 の入力: 移植版が必須の Processor 単体 × それに合うパターン × パターンのオプション（無ければ空の 1 つ）。 */
private data class Stage1Input(val id: ProcessorId, val pattern: CatalogPattern, val options: Map<String, String>)

private fun stage1Inputs(): List<Stage1Input> =
    requiredNative.flatMap { id ->
        catalog.usable
            .filter { !it.background && it.fits(ProcessorSet(listOf(id))) }
            .flatMap { p -> p.options.ifEmpty { listOf(emptyMap()) }.map { Stage1Input(id, p, it) } }
    }

/** 状態のソース（単一モジュール、ソースルートからの相対パス → 中身）。 */
private fun BuildState.sources(): Map<String, String> =
    render(project)
        .modules
        .first()
        .files
        .mapKeys { (p, _) -> p.substringAfter("src/main/kotlin/").substringAfter("src/main/java/") }

class XdStage1DiffTest : DiffSpec({
    case(
        "XD-05",
        "Processor 単体 × カタログの全パターンで、K と N のコンパイル結果（公開 ABI）と実行時テストの結果が一致する（段階 1）",
        pending = PendingCase(Milestone.M2),
    ) {
        val inputs = stage1Inputs()
        // 対象が空なら比べたことにならない（カタログか移植版の一覧が壊れている）
        (inputs.isNotEmpty()) shouldBe true
        acgCheckExhaustive(
            "XD-05 単体 × 全パターンで K と N が段階 1 で一致",
            inputs,
            id = { "pattern:${it.pattern.id} processor:${it.id} options:${it.options.toSortedMap()}" },
        ) { input ->
            val sources = catalog.sources(input.pattern)
            val ids = listOf(input.id)
            val k = runStage(TestEngine.Ksp2, sources, ids, input.options)
            val n = runStage(TestEngine.AnalysisCodegen, sources, ids, input.options)
            withClue("${input.pattern.id} @ ${input.id} ${input.options}") { stage1ShouldMatch(k, n, sources) }
        }
    }
})

class XdStage1IncrementalPbtTest : PbtSpec({
    case(
        "XD-10",
        "変更の列の各ステップで、N の増分結果 = N の最終状態のクリーンビルド、かつ K と段階 1 で一致",
        pending = PendingCase(Milestone.M2),
    ) {
        // 移植版が必須の Processor の組で、途中で Processor を差し替えない列（testing のシナリオの制約。XD-09 と同じ、ADR-037）
        val histories =
            Arb
                .changeHistory(processors = ProcessorSet(listOf(ProcessorId.KOI, ProcessorId.CRM)))
                .filter { h -> h.ops.none { it is ChangeOp.ToggleProcessor } }
        acgCheckAll("XD-10 N の増分 = N のクリーン、K と段階 1 で一致", histories, weight = 2.0) { history ->
            val states = history.states()
            val ids = states.first().processors.order
            val incremental =
                runCodegenScenario(TestEngine.AnalysisCodegen) {
                    initial {
                        loadSources(states.first().sources(), ids)
                        processorsFor(TestEngine.AnalysisCodegen, ids)
                        options.putAll(states.first().options.values)
                    }
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
                val sources = state.sources()
                val nClean = runStage(TestEngine.AnalysisCodegen, sources, ids, state.options.values)
                val kClean = runStage(TestEngine.Ksp2, sources, ids, state.options.values)
                withClue("${step.stepName}\n$state") {
                    // N の増分 = N のクリーン（生成物そのもの）
                    step.result.isSuccess shouldBe nClean.isSuccess
                    step.result.generatedFiles shouldBe nClean.generatedFiles
                    // N のクリーンと K のクリーンは段階 1 で一致
                    stage1ShouldMatch(kClean, nClean, sources)
                }
            }
        }
    }
})

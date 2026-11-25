package me.tbsten.analysiscodegen.testing

/**
 * 変更を順に当てて再実行するシナリオ（TST-05）。各ステップの結果を順に返す。
 *
 * ```
 * val results = runCodegenScenario(TestEngine.AnalysisCodegen) {
 *     initial { kotlin("A.kt", "@Hello class A"); ksp(HelloProcessorProvider()) }
 *     step("B を足す") { kotlin("B.kt", "@Hello class B") }
 *     step("A を消す") { delete("A.kt") }
 * }
 * ```
 *
 * - [TestEngine.AnalysisCodegen] では同じ作業ディレクトリで続けて実行するので、2 回目以降は増分になる（エンジン実装後）
 * - [TestEngine.Ksp2] では毎回クリーンに実行する。増分の結果と比べる基準（「増分 = クリーン」）に使える
 */
public fun runCodegenScenario(
    engine: TestEngine = TestEngine.Ksp2,
    block: CodegenScenarioSpec.() -> Unit,
): List<CodegenStepResult> {
    val scenario = CodegenScenarioSpec().apply(block)
    val spec = checkNotNull(scenario.initial) { "initial { } を書く" }
    val results = mutableListOf<CodegenStepResult>()
    results += CodegenStepResult("initial", runSpec(engine, spec.copy()))
    for ((name, change) in scenario.steps) {
        change.applyTo(spec)
        results += CodegenStepResult(name, runSpec(engine, spec.copy()))
    }
    return results
}

/** シナリオの 1 ステップの結果。 */
public data class CodegenStepResult(val stepName: String, val result: CodegenTestResult)

/** [runCodegenScenario] の DSL。 */
public class CodegenScenarioSpec internal constructor() {
    internal var initial: CodegenTestSpec? = null
    internal val steps = mutableListOf<Pair<String, CodegenChange>>()

    /** 最初の入力。 */
    public fun initial(block: CodegenTestSpec.() -> Unit) {
        check(initial == null) { "initial { } は 1 回だけ" }
        initial = CodegenTestSpec().apply(block)
    }

    /** 変更を 1 つ当てて再実行する。 */
    public fun step(
        name: String,
        block: CodegenChange.() -> Unit,
    ) {
        steps += name to CodegenChange().apply(block)
    }
}

/** 1 ステップで当てる変更。同じパスへの [kotlin] は上書き（= 変更）になる。 */
public class CodegenChange internal constructor() {
    private val edits = mutableListOf<(CodegenTestSpec) -> Unit>()

    /** Kotlin ファイルを足すか書き換える。 */
    public fun kotlin(
        path: String,
        content: String,
        sourceSet: String = CodegenTestSpec.MAIN,
    ) {
        edits += { spec -> spec.replace(sourceSet) { kotlin(path, content) } }
    }

    /** Java ファイルを足すか書き換える。 */
    public fun java(
        path: String,
        content: String,
        sourceSet: String = CodegenTestSpec.MAIN,
    ) {
        edits += { spec -> spec.replace(sourceSet) { java(path, content) } }
    }

    /** ファイルを消す。[path] は拡張子込み。 */
    public fun delete(
        path: String,
        sourceSet: String = CodegenTestSpec.MAIN,
    ) {
        edits += { spec ->
            val files = spec.sourceSets.getValue(sourceSet).files
            check(files.removeIf { it.path == path }) { "$sourceSet に $path が無い" }
        }
    }

    /** オプションを変える。null で消す。 */
    public fun option(
        key: String,
        value: String?,
    ) {
        edits += { spec -> if (value == null) spec.options.remove(key) else spec.options[key] = value }
    }

    internal fun applyTo(spec: CodegenTestSpec) = edits.forEach { it(spec) }

    private fun CodegenTestSpec.replace(
        sourceSet: String,
        add: TestSourceSet.() -> Unit,
    ) {
        sourceSet(sourceSet) {
            val before = files.toList()
            add()
            val added = files.last()
            files.clear()
            files += before.filterNot { it.path == added.path } + added
        }
    }
}

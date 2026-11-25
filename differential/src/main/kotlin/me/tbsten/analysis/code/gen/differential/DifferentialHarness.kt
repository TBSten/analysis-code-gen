package me.tbsten.analysis.code.gen.differential

/**
 * 三者差分ハーネス（ACG-42、ADR-023）。同じ [DiffInput] を K / B / N で動かし、正規化して K と比べる。
 *
 * ```
 * val result = DifferentialHarness.default().run(input)
 * result.assertRequiredMatch()   // 必須の観点で K と違えば AssertionError（差分付き）
 * ```
 *
 * - K は基準。K が動かなければ比較はすべて飛ばす
 * - B / N が [EngineRun.Unavailable] のときはその比較を飛ばす（[PairComparison.skippedReason]）。
 *   飛ばしたことは [DiffResult.report] に出る。「必ず B と比べる」ケースは [DiffResult.requireCompared] で落とす
 * - [listener] は計測口。ベンチマークや試行数の調整が 1 回ごとの時間とキャッシュの当たりを受け取る
 */
public class DifferentialHarness(
    public val runners: List<EngineRunner>,
    public val normalizer: Normalizer = Normalizer.default(),
    public val policies: Map<EngineKind, ComparisonPolicy> =
        mapOf(EngineKind.B to ComparisonPolicy.forBridge(), EngineKind.N to ComparisonPolicy.forNative()),
    private val listener: HarnessListener = HarnessListener.None,
) {
    init {
        require(runners.count { it.engine == EngineKind.K } == 1) { "K のランナーをちょうど 1 つ渡す" }
        require(runners.map { it.engine }.distinct().size == runners.size) { "同じ実行系のランナーが重なっている" }
    }

    /** 3 つ（渡したランナー全部）を動かして比べる。 */
    public fun run(input: DiffInput): DiffResult {
        val runs = runners.associate { r -> r.engine to runEngine(r, input) }
        val normalized =
            runs.mapValues { (_, run) -> (run as? EngineRun.Completed)?.output?.let(normalizer::normalize) }
        val k = normalized[EngineKind.K]
        val comparisons =
            runners.map { it.engine }.filter { it != EngineKind.K }.map { other ->
                val policy = policies[other] ?: ComparisonPolicy(required = emptySet())
                val o = normalized[other]
                when {
                    k == null -> PairComparison(EngineKind.K, other, policy, emptyList(), "K が動かなかった: ${runs[EngineKind.K].reason()}")
                    o == null -> PairComparison(EngineKind.K, other, policy, emptyList(), runs[other].reason())
                    else -> PairComparison(EngineKind.K, other, policy, OutputComparator.compare(k, o, policy.all))
                }
            }
        return DiffResult(input, runs, normalized.filterValues { it != null }.mapValues { it.value!! }, comparisons)
    }

    /** 1 つの実行系だけを動かす（XD-02 の「K で通るか」など）。正規化はしない。 */
    public fun runEngine(
        engine: EngineKind,
        input: DiffInput,
    ): EngineRun = runEngine(runners.single { it.engine == engine }, input)

    private fun runEngine(
        runner: EngineRunner,
        input: DiffInput,
    ): EngineRun = runner.run(input).also { listener.onRun(input, it) }

    public companion object {
        /**
         * 既定の構成: K（ファイルの K 結果キャッシュ付き）・B・N、既定の正規化、M2 の比較の強さ。
         * [cache] に null を渡すとキャッシュを使わない。
         */
        public fun default(
            cache: KResultStore? = FileKResultStore.default(),
            listener: HarnessListener = HarnessListener.None,
        ): DifferentialHarness =
            DifferentialHarness(
                runners = listOf(KspReferenceRunner(cache), BridgeRunner(), NativeRunner()),
                listener = listener,
            )
    }
}

/** 計測口（ADR-023）。 */
public fun interface HarnessListener {
    public fun onRun(
        input: DiffInput,
        run: EngineRun,
    )

    public companion object {
        public val None: HarnessListener = HarnessListener { _, _ -> }
    }
}

/** [DifferentialHarness.run] の結果。 */
public data class DiffResult(
    val input: DiffInput,
    val runs: Map<EngineKind, EngineRun>,
    /** 動いた実行系の正規化後の出力。 */
    val normalized: Map<EngineKind, RunOutput>,
    val comparisons: List<PairComparison>,
) {
    public fun comparison(engine: EngineKind): PairComparison = comparisons.single { it.right == engine }

    /** 必須の観点で K と違うところが無ければ何もしない。あれば [report] を付けて [AssertionError]。 */
    public fun assertRequiredMatch() {
        if (comparisons.any { it.requiredMismatches.isNotEmpty() }) throw AssertionError(report())
    }

    /** [engines] の比較が飛ばされていたら [AssertionError]（「B が動いて一致する」ことまで求めるケース用）。 */
    public fun requireCompared(vararg engines: EngineKind) {
        val skipped = comparisons.filter { it.right in engines && it.skipped }
        if (skipped.isNotEmpty()) {
            throw AssertionError("比較できなかった: " + skipped.joinToString { "K↔${it.right}（${it.skippedReason}）" })
        }
    }

    /** 人が読む差分の報告。 */
    public fun report(): String =
        buildString {
            appendLine("三者差分: ${input.name}（Processor: ${input.processors.joinToString { "${it.id}@${it.version}" }}）")
            runs.values.forEach { run ->
                when (run) {
                    is EngineRun.Completed ->
                        appendLine(
                            "  ${run.engine}（${run.engine.label}）: ${if (run.output.success) "成功" else "失敗"}、" +
                                "生成 ${run.output.files.size} 件、${run.metrics.durationMillis} ms" +
                                if (run.metrics.cacheHit) "（キャッシュ）" else "",
                        )
                    is EngineRun.Unavailable -> appendLine("  ${run.engine}（${run.engine.label}）: 動かせない（${run.reason}）")
                }
            }
            comparisons.forEach { c ->
                when {
                    c.skipped -> appendLine("K↔${c.right}: 比較なし（${c.skippedReason}）")
                    c.mismatches.isEmpty() -> appendLine("K↔${c.right}: 一致（${c.policy.all.joinToString { it.label }}）")
                    else -> {
                        appendLine("K↔${c.right}: 必須の不一致 ${c.requiredMismatches.size} 件、計測のみの不一致 ${c.measuredMismatches.size} 件")
                        c.mismatches.forEach { m ->
                            val tag = if (m.aspect in c.policy.required) "必須" else "計測"
                            appendLine("  [$tag] ${m.aspect.label}: ${m.subject}")
                            m.detail.lines().forEach { appendLine("    $it") }
                        }
                    }
                }
            }
        }
}

private fun EngineRun?.reason(): String =
    when (this) {
        is EngineRun.Unavailable -> reason
        is EngineRun.Completed -> "動いた"
        null -> "ランナーが無い"
    }

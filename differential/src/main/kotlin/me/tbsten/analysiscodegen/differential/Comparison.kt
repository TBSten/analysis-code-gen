package me.tbsten.analysiscodegen.differential

/** 比べる観点（pbt-differential-design.md 3.1）。 */
public enum class Aspect(public val label: String) {
    BuildResult("ビルドの成否"),
    GeneratedSources("生成コード"),
    GeneratedResources("生成リソース"),
    Diagnostics("コンパイラ診断"),
    Logs("Logger 出力"),
    LogLevelsAndLocations("Logger のレベルと位置"),
    Rounds("ラウンドの挙動"),
}

/**
 * K と相手（B か N）を比べるときの強さ。[required] の不一致はテストを落とし、[measured] は差分を出すだけ（表示・集計用）。
 * マイルストーンごとの既定値は [forBridge] / [forNative]（pbt-differential-design.md 3.1・3.2）。
 */
public data class ComparisonPolicy(
    val required: Set<Aspect>,
    val measured: Set<Aspect> = emptySet(),
) {
    public val all: Set<Aspect> get() = required + measured

    public companion object {
        /** K ↔ B。M2: 成否・生成コード・リソース・診断が必須、Logger とラウンドは計測。M3: Logger とラウンドも必須。 */
        public fun forBridge(milestone: DiffMilestone = DiffMilestone.M2): ComparisonPolicy =
            when (milestone) {
                DiffMilestone.M2 ->
                    ComparisonPolicy(
                        required = setOf(Aspect.BuildResult, Aspect.GeneratedSources, Aspect.GeneratedResources, Aspect.Diagnostics),
                        measured = setOf(Aspect.Logs, Aspect.Rounds),
                    )
                DiffMilestone.M3 ->
                    ComparisonPolicy(
                        required =
                            setOf(
                                Aspect.BuildResult,
                                Aspect.GeneratedSources,
                                Aspect.GeneratedResources,
                                Aspect.Diagnostics,
                                Aspect.Logs,
                                Aspect.Rounds,
                            ),
                    )
            }

        /**
         * K ↔ N。M2（段階 1）: 成否・リソース・診断が必須、生成コードと Logger は計測（公開 ABI と実行時テストの比較は XD-05 で足す）。
         * M3（段階 2）: 正規化後の生成コードと Logger のレベル・位置も必須、文言は計測。ラウンドは比べない。
         */
        public fun forNative(milestone: DiffMilestone = DiffMilestone.M2): ComparisonPolicy =
            when (milestone) {
                DiffMilestone.M2 ->
                    ComparisonPolicy(
                        required = setOf(Aspect.BuildResult, Aspect.GeneratedResources, Aspect.Diagnostics),
                        measured = setOf(Aspect.GeneratedSources, Aspect.LogLevelsAndLocations, Aspect.Logs),
                    )
                DiffMilestone.M3 ->
                    ComparisonPolicy(
                        required =
                            setOf(
                                Aspect.BuildResult,
                                Aspect.GeneratedSources,
                                Aspect.GeneratedResources,
                                Aspect.Diagnostics,
                                Aspect.LogLevelsAndLocations,
                            ),
                        measured = setOf(Aspect.Logs),
                    )
            }
    }
}

/** 比較の強さを選ぶマイルストーン。 */
public enum class DiffMilestone { M2, M3 }

/** 観点 1 つの不一致。[detail] は読みやすい差分（先頭の違う行と前後の文脈）。 */
public data class Mismatch(
    val aspect: Aspect,
    val subject: String,
    val detail: String,
)

/** K と相手 1 つの比較結果。どちらかが動かなかったら [skippedReason] に理由が入り、[mismatches] は空。 */
public data class PairComparison(
    val left: EngineKind,
    val right: EngineKind,
    val policy: ComparisonPolicy,
    val mismatches: List<Mismatch>,
    val skippedReason: String? = null,
) {
    public val requiredMismatches: List<Mismatch> get() = mismatches.filter { it.aspect in policy.required }
    public val measuredMismatches: List<Mismatch> get() = mismatches.filter { it.aspect !in policy.required }
    public val skipped: Boolean get() = skippedReason != null
}

/** 正規化済みの 2 つの出力を観点ごとに比べる。 */
public object OutputComparator {
    public fun compare(
        left: RunOutput,
        right: RunOutput,
        aspects: Set<Aspect>,
    ): List<Mismatch> =
        buildList {
            if (Aspect.BuildResult in aspects && left.success != right.success) {
                add(Mismatch(Aspect.BuildResult, "success", "${left.success} ≠ ${right.success}"))
            }
            if (Aspect.GeneratedSources in aspects) addAll(compareFiles(Aspect.GeneratedSources, left.sources, right.sources))
            if (Aspect.GeneratedResources in aspects) addAll(compareFiles(Aspect.GeneratedResources, left.resources, right.resources))
            if (Aspect.Diagnostics in aspects) {
                compareLines(Aspect.Diagnostics, "diagnostics", left.compilerDiagnostics(), right.compilerDiagnostics())?.let(::add)
            }
            if (Aspect.Logs in aspects) {
                compareLines(Aspect.Logs, "logs", left.logs.map { it.render() }, right.logs.map { it.render() })?.let(::add)
            }
            if (Aspect.LogLevelsAndLocations in aspects) {
                val l = left.logs.map { "${it.severity} ${it.location ?: "-"}" }
                val r = right.logs.map { "${it.severity} ${it.location ?: "-"}" }
                compareLines(Aspect.LogLevelsAndLocations, "logs", l, r)?.let(::add)
            }
            if (Aspect.Rounds in aspects && left.rounds != null && right.rounds != null) {
                val l = left.rounds.map { "round ${it.index}: ${it.generatedFiles.joinToString(", ")}" }
                val r = right.rounds.map { "round ${it.index}: ${it.generatedFiles.joinToString(", ")}" }
                compareLines(Aspect.Rounds, "rounds", l, r)?.let(::add)
            }
        }

    // コンパイラの Info / Logging（KSP2 の「API の機能を有効にしていない」などのお知らせ）は実行系ごとに違って当然なので比べない。
    // 比べるのはエラーと警告だけ（Processor の Logger の出力は全レベルを Logs で比べる）
    private fun RunOutput.compilerDiagnostics() =
        diagnostics
            .filter { !it.fromProcessor && it.severity <= DiagnosticRecord.Severity.Warning }
            .map { it.render() }

    private fun DiagnosticRecord.render() = "$severity ${location ?: "-"} $message"

    private fun compareFiles(
        aspect: Aspect,
        left: Map<String, String>,
        right: Map<String, String>,
    ): List<Mismatch> =
        buildList {
            for (path in (left.keys + right.keys).sorted()) {
                val l = left[path]
                val r = right[path]
                when {
                    l == null -> add(Mismatch(aspect, path, "K に無いファイルが相手にだけある"))
                    r == null -> add(Mismatch(aspect, path, "K にあるファイルが相手に無い"))
                    l != r -> compareLines(aspect, path, l.lines(), r.lines())?.let(::add)
                }
            }
        }

    private fun compareLines(
        aspect: Aspect,
        subject: String,
        left: List<String>,
        right: List<String>,
    ): Mismatch? {
        if (left == right) return null
        val first = left.indices.firstOrNull { it >= right.size || left[it] != right[it] } ?: left.size
        val from = (first - CONTEXT).coerceAtLeast(0)

        fun excerpt(lines: List<String>) =
            (from until minOf(lines.size, first + CONTEXT + 1)).joinToString("\n") { i ->
                val mark = if (i == first) ">" else " "
                "$mark ${i + 1}: ${lines[i]}"
            }
        val detail =
            "${first + 1} 行目から違う（K ${left.size} 行 / 相手 ${right.size} 行）\n" +
                "--- K\n${excerpt(left).ifEmpty { "  (ここで終わり)" }}\n" +
                "+++ 相手\n${excerpt(right).ifEmpty { "  (ここで終わり)" }}"
        return Mismatch(aspect, subject, detail)
    }

    private const val CONTEXT = 3
}

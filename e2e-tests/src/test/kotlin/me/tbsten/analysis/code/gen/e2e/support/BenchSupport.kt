package me.tbsten.analysis.code.gen.e2e.support

import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files

// ACG-46 の補助（PERF・MEM・RW-ALL-P1）。計測の決まりは ADR-037
// - 「同じ Processor・同じ入力・同じマシン」で K（KSP2）を 100% とし、比べる側が 100% 以下か
// - ウォームアップ 3 回のあと 10 回計測した中央値（一覧 5.25）
// - 計測するのは `clean acgCodegen` の壁時計。acgCodegen は初期化スクリプトが足す集約タスクで、その実行系の
//   生成タスク（テスト用を除く）すべてに依存する。Gradle 自体の固定の手間は両側に同じだけ入るので「100% 以下か」の判定は変わらない
//   （75% のストレッチ目標の表示だけは固定の手間の分だけ 100% 寄りになる）

/** 生成タスクの集約・ヒープの計測タスクを足す初期化スクリプト。構成キャッシュと両立する書き方にする。 */
private val INIT_SCRIPT =
    """
    allprojects {
        val engine = providers.gradleProperty("acg.fixture.engine").getOrElse("native")
        val prefix = if (engine == "ksp") "ksp" else "analysisCodeGen"
        // -Pacg.bench.tasks=<正規表現> で対象の生成タスクを絞る（PERF-07 の「共通 + 3 プラットフォーム」など）
        val only = providers.gradleProperty("acg.bench.tasks").orNull?.let(::Regex)
        val codegen = tasks.register("acgCodegen") {
            group = "acg"
            dependsOn(
                tasks.matching {
                    it.name.startsWith(prefix) && !it.name.contains("Test") && (only == null || only.matches(it.name))
                },
            )
        }
        // ヒープの計測（MEM-02・03）。acgHeapReset → 生成 → acgHeap の順に流す
        val reset = tasks.register("acgHeapReset") {
            doLast { java.lang.management.ManagementFactory.getMemoryPoolMXBeans().forEach { it.resetPeakUsage() } }
        }
        tasks.register("acgHeap") {
            mustRunAfter(codegen, reset)
            doLast {
                System.gc()
                val pools = java.lang.management.ManagementFactory.getMemoryPoolMXBeans().filter { it.type == java.lang.management.MemoryType.HEAP }
                val used = pools.sumOf { it.usage.used }
                val peak = pools.sumOf { it.peakUsage.used }
                println("ACG_HEAP used=" + used + " peak=" + peak)
            }
        }
        codegen.configure { mustRunAfter(reset) }
    }
    """.trimIndent()

internal val benchInitScript: File by lazy {
    Files.createTempFile("acg-bench", ".init.gradle.kts").toFile().apply {
        writeText(INIT_SCRIPT)
        deleteOnExit()
    }
}

/** 計測結果。[samplesMillis] は計測した回（ウォームアップを除く）の壁時計。 */
internal data class Timing(val label: String, val samplesMillis: List<Long>) {
    val medianMillis: Double
        get() =
            samplesMillis.sorted().let { s ->
                if (s.size % 2 ==
                    1
                ) {
                    s[s.size / 2].toDouble()
                } else {
                    (s[s.size / 2 - 1] + s[s.size / 2]) / 2.0
                }
            }
}

internal const val WARMUP_RUNS = 3
internal const val MEASURED_RUNS = 10

/** [block] を [warmup] 回流したあと [runs] 回計測する。 */
internal fun measure(
    label: String,
    warmup: Int = WARMUP_RUNS,
    runs: Int = MEASURED_RUNS,
    block: () -> Unit,
): Timing {
    repeat(warmup) { block() }
    val samples =
        List(runs) {
            val start = System.nanoTime()
            block()
            (System.nanoTime() - start) / 1_000_000
        }
    return Timing(label, samples)
}

/** プロジェクトを [engine] で動かす TestKit。初期化スクリプトで acgCodegen・acgHeap を足す。 */
internal fun benchRunner(
    projectDir: File,
    engine: SampleEngine,
    vararg tasks: String,
): GradleRunner =
    GradleRunner
        .create()
        .withProjectDir(projectDir)
        .withArguments(tasks.toList() + listOf("--init-script", benchInitScript.absolutePath, "--no-build-cache") + gradleArgs(engine))

/** daemon を止める。次のビルドは新しい daemon で始まる（Cold start の計測。依存のキャッシュは残る）。 */
internal fun stopDaemons(projectDir: File) {
    GradleRunner
        .create()
        .withProjectDir(projectDir)
        .withArguments("--stop")
        .build()
}

/** 全件処理の時間: `clean acgCodegen` を計測する。 */
internal fun measureFullCodegen(
    projectDir: File,
    engine: SampleEngine,
    label: String = "${projectDir.name}/$engine",
): Timing = measure(label) { benchRunner(projectDir, engine, "clean", "acgCodegen").build() }

/** [subject] が [baseline]（KSP2）の 100% 以下。比を build/acg/bench に残す（進捗レポートが 75% のストレッチ目標と並べて表示する）。 */
internal fun shouldNotBeSlowerThan(
    caseId: String,
    subject: Timing,
    baseline: Timing,
) {
    val ratio = subject.medianMillis / baseline.medianMillis
    recordBench(caseId, subject, baseline, ratio)
    withClue(
        "$caseId: ${subject.label} ${subject.medianMillis}ms / ${baseline.label} ${baseline.medianMillis}ms = ${"%.3f".format(ratio)}",
    ) {
        ratio shouldBeLessThanOrEqual 1.0
    }
}

/** `build/acg/bench/results.tsv` に 1 行足す: ケース ID・比べる側・基準・中央値 2 つ・比。 */
internal fun recordBench(
    caseId: String,
    subject: Timing,
    baseline: Timing,
    ratio: Double,
) {
    val file = repoRoot.resolve("build/acg/bench/results.tsv")
    file.parentFile.mkdirs()
    if (!file.exists()) file.writeText("case\tsubject\tbaseline\tsubjectMedianMs\tbaselineMedianMs\tratio\n")
    file.appendText(
        "$caseId\t${subject.label}\t${baseline.label}\t${subject.medianMillis}\t${baseline.medianMillis}\t${"%.4f".format(ratio)}\n",
    )
}

/** `acgHeap` の出力から（使用中, ピーク）のバイト数を読む。 */
internal fun BuildResult.heap(): Pair<Long, Long> {
    val m = Regex("""ACG_HEAP used=(\d+) peak=(\d+)""").findAll(output).lastOrNull() ?: error("acgHeap の出力が無い:\n$output")
    return m.groupValues[1].toLong() to m.groupValues[2].toLong()
}

/** F-* フィクスチャ（fixtures/projects/<name>）を一時ディレクトリに写す。 */
internal fun copyFixtureDir(name: String): File {
    val source = repoRoot.resolve("fixtures/projects/$name")
    check(source.isDirectory) { "フィクスチャ $name が無い" }
    return Files.createTempDirectory("acg-fixture-$name").toFile().also { copyProject(source, it) }
}

/** F-LARGE を生成して写す（fixtures/scripts/gen_large.py）。 */
internal fun generateLarge(
    files: Int,
    deps: Int = 0,
): File {
    val out = Files.createTempDirectory("acg-large-$files").toFile()
    val args = mutableListOf("python3", "fixtures/scripts/gen_large.py", "--files", "$files", "--out", out.absolutePath)
    if (deps > 0) args += listOf("--deps", "$deps")
    val process = ProcessBuilder(args).directory(repoRoot).redirectErrorStream(true).start()
    val log = process.inputStream.bufferedReader().readText()
    check(process.waitFor() == 0) { "gen_large.py が失敗:\n$log" }
    return out
}

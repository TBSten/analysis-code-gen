package me.tbsten.analysiscodegen.e2e.perf

import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysiscodegen.e2e.support.SampleEngine
import me.tbsten.analysiscodegen.e2e.support.Timing
import me.tbsten.analysiscodegen.e2e.support.benchRunner
import me.tbsten.analysiscodegen.e2e.support.copyFixture
import me.tbsten.analysiscodegen.e2e.support.generateLarge
import me.tbsten.analysiscodegen.e2e.support.measure
import me.tbsten.analysiscodegen.e2e.support.measureFullCodegen
import me.tbsten.analysiscodegen.e2e.support.recordBench
import me.tbsten.analysiscodegen.e2e.support.repoRoot
import me.tbsten.analysiscodegen.e2e.support.shouldNotBeSlowerThan
import me.tbsten.analysiscodegen.e2e.support.stopDaemons
import me.tbsten.analysiscodegen.testfoundation.BenchSpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import java.io.File
import java.nio.file.Files

// 5.25 性能（PERF）。基準: 同じ Processor（Bridge 版）・同じ入力・同じマシンで KSP2 を 100% として 100% 以下
// （ウォームアップ後 10 回計測の中央値）。75% はストレッチ目標として build/acg/bench に残して表示するだけ。計測の決まりは ADR-037
// Processor はテスト用 Processor（TP-*）の KSP 版。F-* の build.ksp.gradle.kts（K）と build.bridge.gradle.kts（B）が同じ TP を使う

/** K と B で同じ計測をして、B が K 以下であることを確かめる。 */
private fun bridgeShouldNotBeSlowerThanKsp2(
    caseId: String,
    measureOn: (SampleEngine) -> Timing,
) {
    val k = measureOn(SampleEngine.Ksp2)
    val b = measureOn(SampleEngine.Bridge)
    shouldNotBeSlowerThan(caseId, b, k)
}

/** 増分の計測: 変更を当てる・戻すを交互に繰り返し、毎回の `acgCodegen`（clean しない）を計測する。 */
private fun measureIncremental(
    projectDir: File,
    engine: SampleEngine,
    label: String,
    task: String = "acgCodegen",
    apply: () -> Unit,
    revert: () -> Unit,
): Timing {
    benchRunner(projectDir, engine, "clean", task).build()
    var applied = false
    return measure("$label/$engine") {
        if (applied) revert() else apply()
        applied = !applied
        benchRunner(projectDir, engine, task).build()
    }
}

/** F-LARGE の中の 1 ファイル（決まった位置のもの）。 */
private fun File.someSource(): File =
    walkTopDown().filter { it.isFile && it.extension == "kt" && "/src/main/" in it.invariantSeparatorsPath }.sortedBy { it.path }.elementAt(7)

class PerformanceBenchTest : BenchSpec({
    case("PERF-01", "Cold start（プロセス・ClassLoader・AA 環境の初期化）が KSP2 以下", pending = PendingCase(Milestone.M2)) {
        val dir = copyFixture("jvm")
        bridgeShouldNotBeSlowerThanKsp2("PERF-01") { engine ->
            // 毎回 daemon を止めてから流す。依存のキャッシュは残るので、差はプロセスと初期化の時間になる
            measure("F-JVM cold/$engine") {
                stopDaemons(dir)
                benchRunner(dir, engine, "clean", "acgCodegen").build()
            }
        }
    }

    case("PERF-02", "Warm start（Worker 再利用時）が KSP2 以下", pending = PendingCase(Milestone.M2)) {
        val dir = copyFixture("jvm")
        bridgeShouldNotBeSlowerThanKsp2("PERF-02") { engine -> measureFullCodegen(dir, engine, "F-JVM warm/$engine") }
    }

    case("PERF-03", "全件処理（F-LARGE 1,000 ファイル）が KSP2 以下", pending = PendingCase(Milestone.M2)) {
        val dir = generateLarge(files = 1000)
        bridgeShouldNotBeSlowerThanKsp2("PERF-03") { engine -> measureFullCodegen(dir, engine, "F-LARGE-1000/$engine") }
    }

    case("PERF-04", "5 ラウンドの連鎖が KSP2 以下", pending = PendingCase(Milestone.M2)) {
        // F-JVM に TP-Chain の起点（depth = 5）を 20 個足す。1 つの起点からラウンドごとに 1 段ずつ、5 ラウンド続く
        val dir = copyFixture("jvm")
        val chains =
            (1..20).joinToString("\n") { "@me.tbsten.analysiscodegen.tp.Chain(depth = 5) class PerfChainRoot$it" }
        dir.resolve("src/main/kotlin/fixture/PerfChains.kt").apply { parentFile.mkdirs() }.writeText("package fixture\n\n$chains\n")
        bridgeShouldNotBeSlowerThanKsp2("PERF-04") { engine ->
            val timing = measureFullCodegen(dir, engine, "F-JVM chain5/$engine")
            // 本当に 5 段できている（連鎖が途中で止まって速く見えるのを防ぐ）
            val generated = dir.walkTopDown().filter { it.isFile && it.name.startsWith("PerfChainRoot1Chain") }.map { it.nameWithoutExtension }.toSet()
            generated shouldBe (1..5).map { "PerfChainRoot1Chain$it" }.toSet()
            timing
        }
    }

    case("PERF-05", "1 ファイル変更・追加・削除・依存先変更の増分処理が KSP2 以下", pending = PendingCase(Milestone.M2)) {
        val large = generateLarge(files = 1000)
        val target = large.someSource()
        val original = target.readText()
        val added = target.resolveSibling("PerfAdded.kt")

        bridgeShouldNotBeSlowerThanKsp2("PERF-05") { engine ->
            measureIncremental(large, engine, "modify", apply = { target.appendText("\nfun perfModified() = 1\n") }, revert = { target.writeText(original) })
        }
        bridgeShouldNotBeSlowerThanKsp2("PERF-05") { engine ->
            measureIncremental(
                large,
                engine,
                "add",
                apply = { added.writeText("package ${original.lineSequence().first { it.startsWith("package ") }.removePrefix("package ")}\n\nclass PerfAdded\n") },
                revert = { added.delete() },
            )
        }
        bridgeShouldNotBeSlowerThanKsp2("PERF-05") { engine ->
            measureIncremental(large, engine, "delete", apply = { target.delete() }, revert = { target.writeText(original) })
        }
        // 依存先変更: F-JVM-MULTI の :lib を変え、:app の生成を計測する
        val multi = copyFixture("jvm-multi")
        val libFile = multi.resolve("lib/src/main/kotlin").walkTopDown().first { it.isFile && it.extension == "kt" }
        val libOriginal = libFile.readText()
        bridgeShouldNotBeSlowerThanKsp2("PERF-05") { engine ->
            measureIncremental(
                multi,
                engine,
                "dependency",
                task = ":app:acgCodegen",
                apply = { libFile.appendText("\nfun perfLibChanged() = 1\n") },
                revert = { libFile.writeText(libOriginal) },
            )
        }
    }

    case("PERF-06", "大規模（5,000 ファイル・依存 300 個）の全件処理が KSP2 以下", pending = PendingCase(Milestone.M2)) {
        val dir = generateLarge(files = 5000, deps = 300)
        bridgeShouldNotBeSlowerThanKsp2("PERF-06") { engine -> measureFullCodegen(dir, engine, "F-LARGE-5000-deps300/$engine") }
    }

    case("PERF-07", "KMP（共通 + 3 プラットフォーム）の合計時間が KSP2 以下", pending = PendingCase(Milestone.M2)) {
        // F-KMP の commonMain・jvm・js・linuxX64 の生成タスクだけを集約して計測する（Apple は mac ランナーが要るので入れない）
        val dir = copyFixture("kmp")
        val filter =
            mapOf(
                SampleEngine.Ksp2 to "ksp(CommonMainKotlinMetadata|KotlinJvm|KotlinJs|KotlinLinuxX64)",
                SampleEngine.Bridge to "analysisCodeGen(CommonMain|JvmMain|JsMain|LinuxX64Main)(Compilation)?",
            )
        bridgeShouldNotBeSlowerThanKsp2("PERF-07") { engine ->
            measure("F-KMP common+3/$engine") {
                val result = benchRunner(dir, engine, "clean", "acgCodegen", "-Pacg.bench.tasks=${filter.getValue(engine)}").build()
                // 4 つの実行単位すべてが動いている（絞り込みの正規表現が外れて空振りするのを防ぐ）
                result.tasks.count { t -> Regex(".*:${filter.getValue(engine)}").matches(t.path) } shouldBe 4
            }
        }
    }

    case("PERF-08", "no-op ビルドのオーバーヘッドが KSP2 以下", pending = PendingCase(Milestone.M2)) {
        val dir = copyFixture("jvm")
        bridgeShouldNotBeSlowerThanKsp2("PERF-08") { engine ->
            benchRunner(dir, engine, "clean", "acgCodegen").build()
            measure("F-JVM no-op/$engine") {
                val result = benchRunner(dir, engine, "acgCodegen").build()
                // 何も変えていないので、生成タスクは UP-TO-DATE のはず
                result.output shouldContain "UP-TO-DATE"
            }
        }
    }

    case(
        "PERF-09",
        "同じ処理を Native 版で書いたとき、Bridge 版より速い（Bridge のオーバーヘッドを計測して表示）",
        pending = PendingCase(Milestone.M2),
    ) {
        // TP-* の Native 版と KSP 版は同じ生成物を作る（ADR-027）。TP-Body は KSP 版が無いので両側から外す
        val dir = generateLarge(files = 1000)
        val native = measureFullCodegen(dir, SampleEngine.Native, "F-LARGE-1000/Native")
        val bridge = measureFullCodegen(dir, SampleEngine.Bridge, "F-LARGE-1000/Bridge")
        val ratio = native.medianMillis / bridge.medianMillis
        recordBench("PERF-09", native, bridge, ratio)
        withClue("Native ${native.medianMillis}ms / Bridge ${bridge.medianMillis}ms") { ratio shouldBeLessThan 1.0 }
    }

    case("PERF-10", "CI で前回の基準より 10% 以上遅くなったら失敗する（退行検出が動いている）", pending = PendingCase(Milestone.M2)) {
        // 退行検出: scripts/bench/check_regression.py <基準.tsv> <今回.tsv>。ケースごとの中央値を比べ、10% 以上遅いものがあれば終了コード 1
        val script = repoRoot.resolve("scripts/bench/check_regression.py")
        val work = Files.createTempDirectory("acg-bench-regression").toFile()
        val header = "case\tsubject\tbaseline\tsubjectMedianMs\tbaselineMedianMs\tratio\n"
        val base = work.resolve("base.tsv").apply { writeText(header + "PERF-03\tB\tK\t1000\t1100\t0.9091\n") }

        fun check(currentMs: Int): Int {
            val current = work.resolve("current-$currentMs.tsv").apply { writeText(header + "PERF-03\tB\tK\t$currentMs\t1100\t0\n") }
            val process = ProcessBuilder("python3", script.absolutePath, base.absolutePath, current.absolutePath).redirectErrorStream(true).start()
            process.inputStream.bufferedReader().readText()
            return process.waitFor()
        }
        check(1000) shouldBe 0
        check(1099) shouldBe 0
        check(1100) shouldBe 1 // ちょうど 10% 遅い
        check(1500) shouldBe 1
        // CI の定期ベンチがこの script を呼んでいる（呼ばれていなければ検出は動いていない）
        repoRoot.resolve(".github/workflows").listFiles().orEmpty().any { it.readText().contains("scripts/bench/check_regression.py") } shouldBe true
    }
})

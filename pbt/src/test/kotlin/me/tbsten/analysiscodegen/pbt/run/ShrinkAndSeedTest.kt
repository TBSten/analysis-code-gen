package me.tbsten.analysiscodegen.pbt.run

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import me.tbsten.analysiscodegen.pbt.source.KClassDecl
import me.tbsten.analysiscodegen.pbt.source.KotlinFileSpec
import me.tbsten.analysiscodegen.pbt.source.ProjectSpec
import me.tbsten.analysiscodegen.pbt.source.classIds
import me.tbsten.analysiscodegen.pbt.source.projectSpec
import me.tbsten.analysiscodegen.testfoundation.UnitSpec
import java.io.File
import java.nio.file.Files
import kotlin.time.Duration.Companion.seconds

private fun settings(
    dir: File,
    mode: PbtMode = PbtMode.PR,
) = PbtSettings.fromEnvironment { key ->
    when (key) {
        "acg.pbt.mode" -> mode.name.lowercase()
        "acg.pbt.stateDir" -> File(dir, "state").path
        "acg.pbt.reportDir" -> File(dir, "report").path
        else -> null
    }
}

/** 「クラスは 2 個未満」というわざと破れる性質。 */
private suspend fun failingRun(
    s: PbtSettings,
    failures: MutableList<ProjectSpec>,
) = acgCheckAll("PBT-06 わざと落ちる性質", Arb.projectSpec(), prIterations = 200, settings = s, regressions = emptyList()) { spec ->
    if (spec.classIds().size >= 2) {
        failures += spec
        throw AssertionError("クラスが ${spec.classIds().size} 個ある")
    }
}

private fun ProjectSpec.size(): Int =
    modules.size + modules.sumOf { m -> m.files.size } +
        modules.flatMap { it.files }.filterIsInstance<KotlinFileSpec>().flatMap { it.decls }.sumOf { d ->
            if (d is KClassDecl) 1 + d.members.size + d.ctorParams.size + d.annotations.size + d.nested.size else 1
        }

class ShrinkAndSeedTest : UnitSpec({
    case("PBT-06", "失敗したら縮小して最小の再現例を出し、seed を指定すると同じ失敗を再現できる") {
        val dir = Files.createTempDirectory("pbt06").toFile()
        val s = settings(dir)

        // 1. 失敗すると縮小され、最小の再現例（クラスちょうど 2 個・余計なものなし）が出る
        val seen = mutableListOf<ProjectSpec>()
        val failure = shouldThrow<PbtFailure> { failingRun(s, seen) }
        // kotest が報告する反例 = 縮小の最後に落ちた値
        val minimal = seen.last()
        minimal.classIds() shouldHaveSize 2
        minimal.size() shouldBeLessThanOrEqual 5
        minimal.size() shouldBe seen.minOf { it.size() }

        // 2. seed と再現コマンドが例外と記録ファイルに出る
        failure.message!! shouldContain "seed=${failure.regression.seed}"
        failure.message!! shouldContain "-Pacg.pbt.seed=${failure.regression.seed}"
        val reports = File(s.reportDir, "failures")
        File(reports, "new-regressions.tsv").readText() shouldContain RegressionStore.format(failure.regression)
        File(reports, "${safe("PBT-06 わざと落ちる性質")}-${failure.regression.seed}.md").readText().let { it shouldContain "縮小後の反例" }

        // 3. seed を指定すると同じ失敗（同じ最小の反例）を再現できる
        val again = mutableListOf<ProjectSpec>()
        val replay = s.copy(seed = failure.regression.seed, iterations = failure.regression.iterations)
        shouldThrow<PbtFailure> { failingRun(replay, again) }
        again.last() shouldBe minimal

        // 4. 回帰ケースとして保存すれば、PR ごとの実行で最初に再生される
        val replayed = mutableListOf<Long>()
        shouldThrow<PbtFailure> {
            acgCheckAll("PBT-06 回帰", Arb.projectSpec(), prIterations = 1, settings = s, regressions = listOf(failure.regression)) { spec ->
                replayed += 1
                if (spec.classIds().size >= 2) throw AssertionError("再生した")
            }
        }.regression.seed shouldBe failure.regression.seed
    }

    "PR モードは毎回同じ入力を回す（seed が性質の名前で決まる）" {
        val dir = Files.createTempDirectory("pbt-pr").toFile()
        val a = mutableListOf<ProjectSpec>()
        val b = mutableListOf<ProjectSpec>()
        acgCheckAll("決定性", Arb.projectSpec(), prIterations = 10, settings = settings(dir), regressions = emptyList()) { a += it }
        acgCheckAll("決定性", Arb.projectSpec(), prIterations = 10, settings = settings(dir), regressions = emptyList()) { b += it }
        a shouldBe b
        a.size shouldBeGreaterThan 0
    }

    "acg.pbt.only で別の性質は回らない" {
        val dir = Files.createTempDirectory("pbt-only").toFile()
        val s = settings(dir).copy(only = "ほかの性質")
        acgCheckAll("この性質", Arb.projectSpec(), settings = s, regressions = emptyList()) { error("回ってはいけない") }.skipped shouldBe true
    }

    "定期実行は持ち分の時間でバッチを重ねて止まり、1 試行の時間を記録する" {
        val dir = Files.createTempDirectory("pbt-sched").toFile()
        val s =
            settings(
                dir,
                PbtMode.SCHEDULED,
            ).copy(budget = 2.seconds, hardLimit = 4.seconds, runStartEpochMs = System.currentTimeMillis())
        val start = System.currentTimeMillis()
        val summary =
            acgCheckAll("時間で止まる", Arb.projectSpec(), settings = s, regressions = emptyList()) {
                Thread.sleep(5)
            }
        (System.currentTimeMillis() - start) shouldBeLessThan 4_500L
        summary.batches.size shouldBeGreaterThan 1
        summary.batches.first().iterations shouldBe IterationPlanner.CALIBRATION
        val t = TimingStore(s.stateDir).read("時間で止まる")!!
        (t.nanosPerTrial > 5_000_000.0) shouldBe true
        summary.batches.map { it.origin }.distinct() shouldContain "scheduled"
    }
})

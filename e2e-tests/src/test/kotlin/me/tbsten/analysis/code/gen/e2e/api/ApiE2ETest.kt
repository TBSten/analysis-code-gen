package me.tbsten.analysis.code.gen.e2e.api

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysis.code.gen.e2e.support.Fixture
import me.tbsten.analysis.code.gen.e2e.support.fixtureProject
import me.tbsten.analysis.code.gen.e2e.support.readProbe
import me.tbsten.analysis.code.gen.testfoundation.E2ESpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import org.gradle.testkit.runner.TaskOutcome

// 5.3 Native Processor API（API）のうち E2E のケース。I / U は engine-tests
class ApiE2ETest : E2ESpec({
    case("API-04", "logger の error でビルドが失敗し、ファイル名と行番号付きでビルド出力に出る。warn / info も出る", PendingCase(Milestone.M1)) {
        val out =
            kotlin.io.path
                .createTempFile("probe", ".jsonl")
                .toFile()
                .apply { delete() }
        val p =
            fixtureProject(Fixture.JVM) {
                edit("build.gradle.kts") {
                    it.replace("\"Mapper,Registry,Chain,Body,Defer,Resource\"", "\"Probe\"") +
                        "\nanalysisCodeGen {\n" +
                        "    arg(\"tp.probe.out\", \"${out.invariantSeparatorsPath}\")\n" +
                        "    arg(\"tp.probe.log\", \"info:INFO-MARK;warn:WARN-MARK;error:ERROR-MARK\")\n" +
                        "}\n"
                }
            }
        val result = p.buildAndFail("analysisCodeGenMain", "--info")
        result.task(":analysisCodeGenMain")?.outcome shouldBe TaskOutcome.FAILED
        // TP-Probe は allFiles の先頭（辞書順で Main.kt）を指してログを出す。KtFile 全体を指すので行は 1
        val errorLine = result.output.lines().first { it.contains("ERROR-MARK") }
        errorLine shouldContain "e: "
        errorLine shouldContain "fixture/jvm/Main.kt:1"
        result.output.lines().first { it.contains("WARN-MARK") } shouldContain "w: "
        result.output shouldContain "INFO-MARK"
        // error のあとは finish ではなく onError（API-09）
        readProbe(out).map { it.event }.last() shouldBe "onError"
    }
})

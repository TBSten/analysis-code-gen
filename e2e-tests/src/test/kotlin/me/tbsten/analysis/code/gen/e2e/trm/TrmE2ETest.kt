package me.tbsten.analysis.code.gen.e2e.trm

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysis.code.gen.e2e.support.Fixture
import me.tbsten.analysis.code.gen.e2e.support.fixtureProject
import me.tbsten.analysis.code.gen.testfoundation.E2ESpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import org.gradle.testkit.runner.TaskOutcome

// 5.7 停止と出力の検証（TRM）のうち E2E のケース。I は engine-tests
class TrmE2ETest : E2ESpec({
    case("TRM-07", "既存ソースと同じ FQN の宣言を生成したら、原因が分かる診断を出す", PendingCase(Milestone.M2)) {
        val p =
            fixtureProject(Fixture.JVM) {
                // TP-Mapper が作る fixture.jvm.UserMapper を手で書いておく
                write("src/main/kotlin/fixture/jvm/HandWritten.kt", "package fixture.jvm\nobject UserMapper")
            }
        val result = p.buildAndFail("analysisCodeGenMainSourceSet")
        // ADR-014: Validate でエラー。生成物は解析対象・出力に入らない
        result.task(":analysisCodeGenMainSourceSet")?.outcome shouldBe TaskOutcome.FAILED
        val line = result.output.lines().first { it.contains("fixture.jvm.UserMapper") && it.startsWith("e: ") }
        line shouldContain "TP-Mapper"
        line shouldContain "HandWritten.kt"
        p.generated("main").kotlin("fixture/jvm/UserMapper.kt").isFile shouldBe false
    }
})

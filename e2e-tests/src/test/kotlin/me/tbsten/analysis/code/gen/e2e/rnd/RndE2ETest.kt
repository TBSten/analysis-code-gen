package me.tbsten.analysis.code.gen.e2e.rnd

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysis.code.gen.e2e.support.Fixture
import me.tbsten.analysis.code.gen.e2e.support.fixtureProject
import me.tbsten.analysis.code.gen.e2e.support.readProbe
import me.tbsten.analysis.code.gen.testfoundation.E2ESpec
import org.gradle.testkit.runner.TaskOutcome

// 5.5 マルチラウンド（RND）のうち E2E のケース。I は engine-tests
class RndE2ETest : E2ESpec({
    case("RND-02", "3 ラウンド以上の連鎖が最後まで進む") {
        val out =
            kotlin.io.path
                .createTempFile("probe", ".jsonl")
                .toFile()
                .apply { delete() }
        val p =
            fixtureProject(Fixture.JVM) {
                edit("build.gradle.kts") {
                    it.replace("\"Mapper,Registry,Chain,Body,Defer,Resource\"", "\"Mapper,Registry,Chain,Body,Defer,Resource,Probe\"") +
                        "\nanalysisCodeGen { arg(\"tp.probe.out\", \"${out.invariantSeparatorsPath}\") }\n"
                }
                // @Chain(depth = 5) で 5 段（5 ラウンド + 生成の無い最終ラウンド）
                write(
                    "src/main/kotlin/fixture/jvm/Long.kt",
                    "package fixture.jvm\n@me.tbsten.analysis.code.gen.tp.Chain(depth = 5)\nclass LongChain",
                )
            }
        val result = p.build("compileKotlin")
        result.task(":compileKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
        val gen = p.generated("main")
        (1..5).forEach { gen.kotlin("fixture/jvm/LongChainChain$it.kt").isFile shouldBe true }
        gen.kotlin("fixture/jvm/LongChainChain6.kt").isFile shouldBe false
        gen.kotlin("fixture/jvm/WaitsForChainResolved.kt").readText() shouldContain "fixture.jvm.PipelineChain3"
        readProbe(out).filter { it.event == "process" }.mapNotNull { it.round }.max() shouldBe 6
    }
})

package me.tbsten.analysiscodegen.e2e.ana

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import me.tbsten.analysiscodegen.e2e.support.Fixture
import me.tbsten.analysiscodegen.e2e.support.emptyProject
import me.tbsten.analysiscodegen.e2e.support.fixtureProject
import me.tbsten.analysiscodegen.testfoundation.E2ESpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import org.gradle.testkit.runner.TaskOutcome

// 5.4 AA ネイティブな解析能力（ANA）のうち E2E のケース。I は engine-tests
class AnaE2ETest : E2ESpec({
    case("ANA-01", "関数本体の式と呼び出し先を解決して呼び出しグラフを生成し、生成物がコンパイル・実行できる", PendingCase(Milestone.M1)) {
        val p =
            fixtureProject(Fixture.JVM) {
                write(
                    "src/test/kotlin/fixture/jvm/CallGraphTest.kt",
                    """
                    package fixture.jvm

                    import kotlin.test.Test
                    import kotlin.test.assertEquals

                    class CallGraphTest {
                        @Test fun callees() {
                            assertEquals(listOf("fixture.jvm.Service.format", "fixture.jvm.User.<init>"), Service_runCallGraph.callees)
                        }
                    }
                    """,
                )
            }
        val result = p.build("test")
        result.task(":test")?.outcome shouldBe TaskOutcome.SUCCESS
        p.generated("main").kotlin("fixture/jvm/Service_runCallGraph.kt").readText() shouldContain "\"fixture.jvm.User.<init>\""
    }

    case(
        "ANA-09",
        "kotlinx.serialization と Compose の Compiler Plugin を有効にしたプロジェクトで解析が落ちず、plugin が作る宣言（serializer() など）の扱いが ADR どおり",
        PendingCase(Milestone.M2),
    ) {
        // ADR-038: 解析には Compiler Plugin を読み込まない（KSP2 と同じ）。plugin が作る宣言は解析から見えず、解決できない参照として扱う
        val p =
            emptyProject {
                write(
                    "build.gradle.kts",
                    """
                    plugins {
                        alias(libs.plugins.kotlin.jvm)
                        id("org.jetbrains.kotlin.plugin.serialization") version libs.versions.kotlin.get()
                        id("org.jetbrains.kotlin.plugin.compose") version libs.versions.kotlin.get()
                        id("me.tbsten.analysiscodegen")
                    }
                    dependencies {
                        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
                        implementation("org.jetbrains.compose.runtime:runtime:1.9.0")
                    }
                    analysisCodeGen {
                        arg("tp.processors", "Mapper,Defer")
                        unresolvedDeferred = me.tbsten.analysiscodegen.gradle.DeferredSeverity.Warning
                        sourceSets { main { processor(acg.testProcessors.native) } }
                    }
                    """,
                )
                write(
                    "src/main/kotlin/s/Models.kt",
                    """
                    package s

                    import androidx.compose.runtime.Composable
                    import kotlinx.serialization.Serializable
                    import me.tbsten.analysiscodegen.tp.DeferUntil
                    import me.tbsten.analysiscodegen.tp.Mapped

                    @Mapped
                    @Serializable
                    data class Payload(val id: Int, val name: String)

                    // serializer の実装クラス（plugin が作る）は解析から見えない
                    @DeferUntil("s.Payload.${'$'}serializer")
                    class WantsPluginDeclaration

                    @Composable
                    fun Screen(payload: Payload) {
                        println(Payload.serializer().descriptor.serialName + payload.name)
                    }
                    """,
                )
            }
        val result = p.build("build")
        result.task(":compileKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
        p.generated("main").kotlin("s/PayloadMapper.kt").readText() shouldContain "\"name\" to \"kotlin.String\""
        p.generated("main").kotlin("s/WantsPluginDeclarationResolved.kt").isFile shouldBe false
        result.output shouldContain "WantsPluginDeclaration"
        result.output shouldNotContain "Exception"
    }
})

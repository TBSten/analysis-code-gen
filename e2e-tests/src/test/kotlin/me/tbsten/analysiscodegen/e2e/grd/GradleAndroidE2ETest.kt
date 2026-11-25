package me.tbsten.analysiscodegen.e2e.grd

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysiscodegen.e2e.support.Fixture
import me.tbsten.analysiscodegen.e2e.support.emptyProject
import me.tbsten.analysiscodegen.e2e.support.fixtureProject
import me.tbsten.analysiscodegen.e2e.support.plannedTasks
import me.tbsten.analysiscodegen.testfoundation.E2ESpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import org.gradle.testkit.runner.TaskOutcome

private val M1 = PendingCase(Milestone.M1)
private val M2 = PendingCase(Milestone.M2)

private val VARIANTS = listOf("freeDebug", "freeRelease", "paidDebug", "paidRelease")

// 5.1 Gradle / AGP 統合（GRD）のうち Android のケース。F-AND（Variant を Compilation として扱う。ADR-016）。
// Google Maven と Android SDK が要るため、クラウドの Agent 環境では動かず CI の fixtures ジョブで動かす
class GradleAndroidE2ETest : E2ESpec({
    case("GRD-11", "Android アプリで 4 Variant それぞれにタスクが登録され、Variant 固有のソース（src/free/ など）が解析される", M1) {
        val p = fixtureProject(Fixture.ANDROID)
        val tasks = p.build(":app:tasks", "--all").output
        VARIANTS.forEach { tasks shouldContain "analysisCodeGen${it.replaceFirstChar(Char::uppercase)}Compilation" }

        p.build(":app:assemble")
        p.generatedForCompilation("freeDebug", "app").kotlin("fixture/android/app/FreeFeatureMapper.kt").isFile shouldBe true
        p.generatedForCompilation("freeDebug", "app").kotlin("fixture/android/app/PaidFeatureMapper.kt").isFile shouldBe false
        p.generatedForCompilation("paidRelease", "app").kotlin("fixture/android/app/PaidFeatureMapper.kt").isFile shouldBe true
        p.generatedForCompilation("paidRelease", "app").kotlin("fixture/android/app/FreeFeatureMapper.kt").isFile shouldBe false
        p
            .generatedForCompilation("paidDebug", "app")
            .kotlin("me/tbsten/analysiscodegen/tp/generated/TpRegistry.kt")
            .readText() shouldContain "\"fixture.android.app.PaidFeature\""
    }

    case("GRD-12", "Android の boot classpath（android.jar）が解析に入り、android.* の型が解決される", M1) {
        val p = fixtureProject(Fixture.ANDROID)
        p.build(":app:analysisCodeGenFreeDebugCompilation")
        val mapper = p.generatedForCompilation("freeDebug", "app").kotlin("fixture/android/app/DeviceInfoMapper.kt").readText()
        mapper shouldContain "\"context\" to \"android.content.Context?\""
        p.generatedForCompilation("debug", "lib").kotlin("fixture/android/lib/LibModelMapper.kt").readText() shouldContain
            "\"uri\" to \"android.net.Uri?\""
    }

    case("GRD-13", "Android の unit test / androidTest の Compilation にも生成でき、main の生成物がテストから見える", M2) {
        val p = fixtureProject(Fixture.ANDROID)
        val result = p.build(":app:testFreeDebugUnitTest", ":app:compileFreeDebugAndroidTestKotlin")
        result.task(":app:testFreeDebugUnitTest")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":app:compileFreeDebugAndroidTestKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
        // テストの Compilation にもタスクが走る（compilations.all に含まれる）
        result.task(":app:analysisCodeGenFreeDebugUnitTestCompilation")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":app:analysisCodeGenFreeDebugAndroidTestCompilation")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    case("GRD-14", "AGP の Variant API（androidComponents.onVariants）経由で生成ソースが登録される", M2) {
        val p =
            fixtureProject(Fixture.ANDROID) {
                edit("app/build.gradle.kts") {
                    it +
                        """

                        androidComponents {
                            onVariants { variant ->
                                val name = variant.name
                                val dirs = variant.sources.kotlin?.all
                                tasks.register("printSources" + name.replaceFirstChar(Char::uppercase)) {
                                    val files = dirs?.map { list -> list.map { d -> d.asFile.invariantSeparatorsPath } }
                                    doLast { files?.get()?.forEach { f -> println("SRC=" + f) } }
                                }
                            }
                        }
                        """.trimIndent()
                }
            }
        val out = p.build(":app:printSourcesPaidRelease").output
        out.lines().any { it.startsWith("SRC=") && it.endsWith("build/generated/analysisCodeGen/compilations/paidRelease/kotlin") } shouldBe
            true
        // Variant API 経由で登録したので、AGP が生成タスクを compile の前に並べる
        p.plannedTasks(":app:compilePaidReleaseKotlin").shouldContainInOrder(
            ":app:analysisCodeGenPaidReleaseCompilation",
            ":app:compilePaidReleaseKotlin",
        )
    }

    case("GRD-15", "AGP の KMP 用 Android plugin（com.android.kotlin.multiplatform.library）で生成・ビルドできる", M2) {
        val p =
            emptyProject {
                file("src/main").deleteRecursively()
                write(
                    "build.gradle.kts",
                    """
                    plugins {
                        alias(libs.plugins.kotlin.multiplatform)
                        alias(libs.plugins.android.kotlin.multiplatform.library)
                        id("me.tbsten.analysis.code.gen")
                    }
                    kotlin {
                        androidLibrary {
                            namespace = "fixture.kmpandroid"
                            compileSdk = 36
                            minSdk = 24
                        }
                    }
                    analysisCodeGen {
                        arg("tp.processors", "Mapper")
                        sourceSets { androidMain { processor(acg.testProcessors.native) } }
                    }
                    """,
                )
                write(
                    "src/androidMain/kotlin/me/tbsten/analysiscodegen/tp/TpAnnotations.kt",
                    me.tbsten.analysiscodegen.e2e.support
                        .tpAnnotationsSource(),
                )
                write(
                    "src/androidMain/kotlin/k/Screen.kt",
                    """
                    package k
                    @me.tbsten.analysiscodegen.tp.Mapped
                    data class Screen(val context: android.content.Context?)
                    val described = ScreenMapper.propertyTypes
                    """,
                )
            }
        val result = p.build("assemble")
        result.task(":analysisCodeGenAndroidMain")?.outcome shouldBe TaskOutcome.SUCCESS
        p.generated("androidMain").kotlin("k/ScreenMapper.kt").readText() shouldContain "android.content.Context?"
        p
            .build(
                "tasks",
                "--all",
            ).output
            .lines()
            .filter { it.startsWith("analysisCodeGen") }
            .map { it.substringBefore(' ') }
            .shouldContainAll(
                "analysisCodeGenAndroidMain",
            )
    }
})

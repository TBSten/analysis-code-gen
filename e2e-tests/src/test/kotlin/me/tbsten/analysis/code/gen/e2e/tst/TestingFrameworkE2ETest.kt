package me.tbsten.analysis.code.gen.e2e.tst

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysis.code.gen.e2e.support.TestReport
import me.tbsten.analysis.code.gen.e2e.support.copyProject
import me.tbsten.analysis.code.gen.e2e.support.repoRoot
import me.tbsten.analysis.code.gen.testfoundation.E2ESpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import me.tbsten.analysis.code.gen.testing.CodegenTestSpec
import me.tbsten.analysis.code.gen.testing.TestEngine
import me.tbsten.analysis.code.gen.testing.gradle.gradleFixture
import me.tbsten.analysis.code.gen.testing.runCodegenScenario
import me.tbsten.analysis.code.gen.testing.runCodegenTest
import me.tbsten.analysis.code.gen.testprocessors.TpAnnotationsSource
import me.tbsten.analysis.code.gen.testprocessors.ksp.ActualKspProvider
import me.tbsten.analysis.code.gen.testprocessors.ksp.ChainKspProvider
import me.tbsten.analysis.code.gen.testprocessors.ksp.MapperKspProvider
import me.tbsten.analysis.code.gen.testprocessors.ksp.RegistryKspProvider
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.nio.file.Files

// 5.21 テストフレームワーク（TST）。利用者が自分の Processor のテストに使う testing の API（ADR-021）を、利用者と同じ書き方で使う
// Processor はテスト用 Processor（TP-*、ADR-027）の KSP 版。TestEngine.AnalysisCodegen では Bridge 経由で動く
// TST-01（Native Processor）は AA の型に触れるので :engine-tests に置く

private const val USER_SOURCE =
    "package fixture\n\nimport me.tbsten.analysis.code.gen.tp.Mapped\n\n@Mapped class User(val name: String, val age: Int)"

private fun CodegenTestSpec.mapperInput() {
    kotlin("me/tbsten/analysis/code/gen/tp/${TpAnnotationsSource.FILE_NAME}", TpAnnotationsSource.content)
    kotlin("fixture/User.kt", USER_SOURCE)
    ksp(MapperKspProvider())
    options["tp.processors"] = "Mapper"
}

private fun acgVersion(): String =
    repoRoot
        .resolve("gradle.properties")
        .readLines()
        .first { it.startsWith("acg.version=") }
        .substringAfter("=")
        .trim()

class TestingFrameworkE2ETest : E2ESpec({
    case("TST-02", "同じ API で KSP Processor（Bridge）もテストできる", pending = PendingCase(Milestone.M1)) {
        val k = runCodegenTest(TestEngine.Ksp2) { mapperInput() }
        val b = runCodegenTest(TestEngine.AnalysisCodegen) { mapperInput() }

        b.isSuccess shouldBe true
        b.errors.shouldBeEmpty()
        // 同じ入力・同じ Processor なら、Bridge で取れる生成物は KSP2 と同じ
        b.generatedFiles shouldBe k.generatedFiles
        b.generatedFile("UserMapper.kt") shouldContain "public object UserMapper"
    }

    case("TST-03", "生成物をコンパイルし、生成クラスを読み込んで実行できる", pending = PendingCase(Milestone.M1)) {
        val result = runCodegenTest(TestEngine.AnalysisCodegen) { mapperInput() }

        val mapper = result.loadClass("fixture.UserMapper")
        val user = result.loadClass("fixture.User").getConstructor(String::class.java, Int::class.javaPrimitiveType).newInstance("Ann", 7)
        val instance = mapper.getField("INSTANCE").get(null)
        mapper.getMethod("toMap", user.javaClass).invoke(instance, user) shouldBe mapOf("name" to "Ann", "age" to 7)
    }

    case("TST-04", "KMP の SourceSet（common + jvm など）を指定したテストを書ける", pending = PendingCase(Milestone.M2)) {
        // TP-Actual: commonMain の expect fun に、プラットフォームの SourceSet で actual を作る
        val result =
            runCodegenTest(TestEngine.AnalysisCodegen) {
                sourceSet("commonMain") {
                    kotlin("me/tbsten/analysis/code/gen/tp/${TpAnnotationsSource.FILE_NAME}", TpAnnotationsSource.content)
                    kotlin(
                        "fixture/Platform.kt",
                        "package fixture\n\nimport me.tbsten.analysis.code.gen.tp.GenerateActual\n\n@GenerateActual expect fun platformName(): String",
                    )
                }
                sourceSet("jvmMain") { dependsOn += "commonMain" }
                ksp(ActualKspProvider())
                options["tp.processors"] = "Actual"
            }

        result.isSuccess shouldBe true
        // actual は jvmMain の実行単位で 1 回だけ作られ、commonMain には何も作られない
        val actuals = result.generatedFiles.filterValues { "actual fun platformName" in it }
        actuals.size shouldBe 1
        actuals.keys.single() shouldContain "jvmMain"
        result.loadClass("fixture.PlatformKt").getMethod("platformName").invoke(null) shouldBe "jvm"
    }

    case("TST-05", "変更を適用して再実行する、マルチラウンド・増分のシナリオをテストで書ける", pending = PendingCase(Milestone.M2)) {
        fun scenario(engine: TestEngine) =
            runCodegenScenario(engine) {
                initial {
                    kotlin("me/tbsten/analysis/code/gen/tp/${TpAnnotationsSource.FILE_NAME}", TpAnnotationsSource.content)
                    // TP-Chain は depth の数だけラウンドを重ねる（マルチラウンド）
                    kotlin("fixture/A.kt", "package fixture\n\nimport me.tbsten.analysis.code.gen.tp.Chain\n\n@Chain(depth = 3) class A")
                    ksp(ChainKspProvider())
                    ksp(RegistryKspProvider())
                    options["tp.processors"] = "Chain,Registry"
                }
                step(
                    "B を足す",
                ) { kotlin("fixture/B.kt", "package fixture\n\nimport me.tbsten.analysis.code.gen.tp.Registered\n\n@Registered class B") }
                step(
                    "A の段数を変える",
                ) { kotlin("fixture/A.kt", "package fixture\n\nimport me.tbsten.analysis.code.gen.tp.Chain\n\n@Chain(depth = 1) class A") }
                step("B を消す") { delete("fixture/B.kt") }
            }

        val incremental = scenario(TestEngine.AnalysisCodegen)
        val clean = scenario(TestEngine.Ksp2)
        incremental.map { it.stepName } shouldBe listOf("initial", "B を足す", "A の段数を変える", "B を消す")
        incremental
            .first()
            .result.generatedFiles.keys shouldContain "kotlin/fixture/AChain3.kt"
        // 各ステップの増分の結果 = 同じ状態をクリーンに回した KSP2 の結果
        incremental.zip(clean).forEach { (i, c) ->
            i.result.isSuccess shouldBe true
            i.result.generatedFiles shouldBe c.result.generatedFiles
        }
        incremental[2]
            .result.generatedFiles.keys
            .none { it.endsWith("AChain2.kt") } shouldBe true
    }

    case("TST-06", "Gradle TestKit でフィクスチャプロジェクトを作ってビルドする補助がある", pending = PendingCase(Milestone.M2)) {
        // 利用者が自分の Processor を Gradle で試すときの書き方: 補助でプロジェクトを組み、analysisCodeGen の plugin と Processor を足してビルドする
        val repo = repoRoot.resolve("build/fixture-repo")
        val fixture =
            gradleFixture {
                extraPluginRepositories += "maven(\"${repo.invariantSeparatorsPath}\")"
                extraRepositories += "maven(\"${repo.invariantSeparatorsPath}\")"
                buildScript =
                    """
                    plugins {
                        kotlin("jvm") version "{{kotlinVersion}}"
                        id("me.tbsten.analysis.code.gen") version "${acgVersion()}"
                    }

                    analysisCodeGen {
                        arg("tp.processors", "Mapper")
                        sourceSets {
                            main { processor("me.tbsten.analysis.code.gen:test-processors-native:${acgVersion()}") }
                        }
                    }
                    """.trimIndent()
                kotlin("src/main/kotlin/me/tbsten/analysis/code/gen/tp/${TpAnnotationsSource.FILE_NAME}", TpAnnotationsSource.content)
                kotlin("src/main/kotlin/fixture/User.kt", USER_SOURCE)
            }

        val result = fixture.build("compileKotlin")

        result.task(":analysisCodeGenMain")?.outcome shouldBe TaskOutcome.SUCCESS
        fixture.projectDir.resolve("build/generated/analysisCodeGen/main/kotlin/fixture/UserMapper.kt").isFile shouldBe true
        fixture.projectDir.resolve("build/classes/kotlin/main/fixture/UserMapper.class").isFile shouldBe true
    }

    case("TST-07", "公開形式の成果物（mavenLocal）を外部プロジェクトから依存して使える", pending = PendingCase(Milestone.M2)) {
        // 公開形式の成果物は publishFixtureDeps が build/fixture-repo に出す（mavenLocal と同じレイアウト）。外部プロジェクトから
        // testing と TP の KSP 版に依存し、利用者のテストとして runCodegenTest を書いて流す
        val repo = repoRoot.resolve("build/fixture-repo")
        val version = acgVersion()
        val fixture =
            gradleFixture {
                extraRepositories += "maven(\"${repo.invariantSeparatorsPath}\")"
                kotlinJvm(
                    """
                    dependencies {
                        testImplementation("me.tbsten.analysis.code.gen:analysis-code-gen-testing:$version")
                        testImplementation("me.tbsten.analysis.code.gen:test-processors-ksp:$version")
                        testImplementation(kotlin("test"))
                    }
                    tasks.test { useJUnitPlatform() }
                    """,
                )
                kotlin(
                    "src/test/kotlin/UserProcessorTest.kt",
                    """
                    import kotlin.test.Test
                    import kotlin.test.assertTrue
                    import me.tbsten.analysis.code.gen.testing.TestEngine
                    import me.tbsten.analysis.code.gen.testing.runCodegenTest

                    class UserProcessorTest {
                        @Test fun generates() {
                            val result = runCodegenTest(TestEngine.AnalysisCodegen) {
                                kotlin("me/tbsten/analysis/code/gen/tp/TpAnnotations.kt", me.tbsten.analysis.code.gen.testprocessors.TpAnnotationsSource.content)
                                kotlin("fixture/User.kt", "package fixture\n\n@me.tbsten.analysis.code.gen.tp.Mapped class User(val name: String)")
                                ksp(me.tbsten.analysis.code.gen.testprocessors.ksp.MapperKspProvider())
                            }
                            assertTrue(result.isSuccess, result.messages)
                            assertTrue(result.generatedFiles.keys.any { it.endsWith("UserMapper.kt") })
                        }
                    }
                    """,
                )
            }

        val result = fixture.build("test")

        result.task(":test")?.outcome shouldBe TaskOutcome.SUCCESS
        val report = TestReport.read(fixture.projectDir.resolve("build/test-results"))
        report.tests shouldBe 1
        report.failures shouldBe 0
    }

    case("TST-08", "本家 kotlin-compile-testing からの移行手順がドキュメントどおりに動く", pending = PendingCase(Milestone.M3)) {
        // ドキュメントの移行ガイドのコード例の出所（ADR-031）: fixtures/docs/migrate-kct/{before,after}。
        // before は本家 kotlin-compile-testing、after はガイドの手順を当てたもの。同じテストが同じ件数だけ通る
        val docs = repoRoot.resolve("fixtures/docs/migrate-kct")
        val counts =
            listOf("before", "after").associateWith { name ->
                val dir = Files.createTempDirectory("acg-migrate-kct-$name").toFile()
                copyProject(docs.resolve(name), dir)
                val result =
                    GradleRunner
                        .create()
                        .withProjectDir(dir)
                        .withArguments("test", "--stacktrace")
                        .forwardOutput()
                        .build()
                result.task(":test")?.outcome shouldBe TaskOutcome.SUCCESS
                TestReport.read(dir.resolve("build/test-results"))
            }
        counts.getValue("after").tests shouldBeGreaterThan 0
        counts.getValue("after") shouldBe counts.getValue("before")
        // after は本家への依存を残していない
        docs
            .resolve("after")
            .walkTopDown()
            .filter { it.isFile && it.name.endsWith(".kts") }
            .none { "com.github.tschuchortdev" in it.readText() || "dev.zacsweers.kctfork" in it.readText() } shouldBe true
    }
})

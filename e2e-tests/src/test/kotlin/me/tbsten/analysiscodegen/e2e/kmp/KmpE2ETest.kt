package me.tbsten.analysiscodegen.e2e.kmp

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import me.tbsten.analysiscodegen.e2e.support.Fixture
import me.tbsten.analysiscodegen.e2e.support.FixtureProject
import me.tbsten.analysiscodegen.e2e.support.analysisCodegenTaskPaths
import me.tbsten.analysiscodegen.e2e.support.emptyProject
import me.tbsten.analysiscodegen.e2e.support.fixtureProject
import me.tbsten.analysiscodegen.e2e.support.plannedTasks
import me.tbsten.analysiscodegen.e2e.support.readProbe
import me.tbsten.analysiscodegen.e2e.support.tpAnnotationsSource
import me.tbsten.analysiscodegen.testfoundation.E2ESpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import org.gradle.testkit.runner.TaskOutcome

private val M1 = PendingCase(Milestone.M1)
private val M2 = PendingCase(Milestone.M2)

/** F-KMP から Android ターゲットを外す（Android SDK の無い環境でも動かせるように。Android を見るケースでは使わない）。 */
private fun FixtureProject.withoutAndroid() {
    edit("build.gradle.kts") {
        it
            .replace("    alias(libs.plugins.android.kotlin.multiplatform.library)\n", "")
            .replace(Regex("""    androidLibrary \{[^}]*}\n"""), "")
    }
}

/** 全ターゲットの Kotlin コンパイルタスク（F-KMP）。Apple は macOS でだけ動くので [apple] で切り替える。 */
private fun kmpCompileTasks(apple: Boolean): List<String> =
    listOf(":compileKotlinJvm", ":compileKotlinJs", ":compileKotlinWasmJs", ":compileKotlinLinuxX64") +
        if (apple) listOf(":compileKotlinIosArm64", ":compileKotlinIosSimulatorArm64") else emptyList()

private val isMac = System.getProperty("os.name").lowercase().contains("mac")

// 5.2 KMP の整合性（KMP）のうち E2E のケース。I / U は engine-tests と KmpNamingTest
class KmpE2ETest : E2ESpec({
    case("KMP-01", "commonMain の Processor が commonMain に 1 回だけ生成し、全ターゲットの Compilation から参照してビルドが通る", M1) {
        val p = fixtureProject(Fixture.KMP) { withoutAndroid() }
        val result = p.build(*kmpCompileTasks(isMac).toTypedArray(), "allTests")
        result.task(":analysisCodeGenCommonMain")?.outcome shouldBe TaskOutcome.SUCCESS
        kmpCompileTasks(isMac).forEach { result.task(it)?.outcome shouldBe TaskOutcome.SUCCESS }
        p.generated("commonMain").kotlin("fixture/kmp/SharedMapper.kt").isFile shouldBe true
    }

    case("KMP-02", "commonMain の生成物がプラットフォームごとに再生成されない（生成ファイル数と宣言の重複が 0）", M1) {
        val p = fixtureProject(Fixture.KMP) { withoutAndroid() }
        p.build(*kmpCompileTasks(isMac).toTypedArray())
        val all = p.allGenerated()
        // SharedMapper は commonMain の 1 つだけ。Compilation 側（compilations/<Name>）には現れない
        all.keys.filter { it.endsWith("/SharedMapper.kt") } shouldBe listOf("commonMain/kotlin/fixture/kmp/SharedMapper.kt")
        val names = all.keys.filter { it.endsWith(".kt") }.groupBy { it.substringAfter("/kotlin/") }
        // 同じ相対パスが 2 つあるのは Compilation ごとの actual / 集約だけ
        names.filterValues { it.size > 1 }.keys shouldBe
            setOf("fixture/kmp/SharedActual.kt", "me/tbsten/analysiscodegen/tp/generated/TpRegistry.kt")
    }

    case("KMP-03", "expect から各プラットフォームに actual を生成し、全ターゲットでコンパイルが通る", M1) {
        val p = fixtureProject(Fixture.KMP) { withoutAndroid() }
        p.build(*kmpCompileTasks(isMac).toTypedArray(), "jvmTest", "jsNodeTest")
        p.generatedForCompilation("jvmMain").kotlin("fixture/kmp/SharedActual.kt").readText() shouldContain
            "actual fun platformName(): String = \"jvm\""
        p.generatedForCompilation("jsMain").kotlin("fixture/kmp/SharedActual.kt").readText() shouldContain "= \"js\""
        p.generatedForCompilation("wasmJsMain").kotlin("fixture/kmp/SharedActual.kt").readText() shouldContain "= \"wasm\""
        p.generatedForCompilation("linuxX64Main").kotlin("fixture/kmp/SharedActual.kt").readText() shouldContain "= \"native\""
        // commonMain には actual を作らない
        p
            .generated("commonMain")
            .files()
            .keys
            .none { it.endsWith("Actual.kt") } shouldBe true
    }

    case("KMP-04", "同じモジュールで commonMain と jvmMain に別々の Processor を付け、それぞれ自分の own files だけを処理する", M1) {
        val p =
            fixtureProject(Fixture.KMP) {
                withoutAndroid()
                edit("build.gradle.kts") {
                    it.replace(
                        "        commonMain {",
                        "        jvmMain {\n            processor(acg.testProcessors.native)\n            arg(\"tp.processors\", \"Mapper\")\n        }\n        commonMain {",
                    )
                }
                write(
                    "src/jvmMain/kotlin/fixture/kmp/JvmModel.kt",
                    "package fixture.kmp\n@me.tbsten.analysiscodegen.tp.Mapped\ndata class JvmModel(val x: Int)",
                )
            }
        p.build("compileKotlinJvm")
        p.generated("jvmMain").kotlin("fixture/kmp/JvmModelMapper.kt").isFile shouldBe true
        p.generated("jvmMain").kotlin("fixture/kmp/SharedMapper.kt").isFile shouldBe false
        p.generated("commonMain").kotlin("fixture/kmp/JvmModelMapper.kt").isFile shouldBe false
    }

    case("KMP-05", "中間 SourceSet（iosMain / appleMain / nativeMain）に生成し、iosArm64Main・iosSimulatorArm64Main から参照できる", M1) {
        // mac ランナーで動かす
        val p =
            fixtureProject(Fixture.KMP) {
                withoutAndroid()
                edit("build.gradle.kts") {
                    it.replace(
                        "    sourceSets {\n        commonMain {",
                        "    sourceSets {\n        named(\"iosMain\") {\n            processor(acg.testProcessors.native)\n            arg(\"tp.processors\", \"Mapper\")\n        }\n        commonMain {",
                    )
                }
                write(
                    "src/iosMain/kotlin/fixture/kmp/IosModel.kt",
                    "package fixture.kmp\n@me.tbsten.analysiscodegen.tp.Mapped\ndata class IosModel(val x: Int)",
                )
                write("src/iosArm64Main/kotlin/fixture/kmp/UseArm.kt", "package fixture.kmp\nval arm = IosModelMapper.toMap(IosModel(1))")
                write(
                    "src/iosSimulatorArm64Main/kotlin/fixture/kmp/UseSim.kt",
                    "package fixture.kmp\nval sim = IosModelMapper.toMap(IosModel(1))",
                )
            }
        val result = p.build("compileKotlinIosArm64", "compileKotlinIosSimulatorArm64")
        result.task(":analysisCodeGenIosMain")?.outcome shouldBe TaskOutcome.SUCCESS
        p.generated("iosMain").kotlin("fixture/kmp/IosModelMapper.kt").isFile shouldBe true
        p.allGenerated().keys.count { it.endsWith("IosModelMapper.kt") } shouldBe 1
    }

    case("KMP-06", "解析の可視範囲が正しい: commonMain から java.* は解決できず、iosMain から platform.UIKit は解決でき JVM API は解決できない", M1) {
        // mac ランナーで動かす。TP-Defer は解決できない target を最後まで持ち越し、エラーで一覧を出す（ADR-014）
        val p =
            fixtureProject(Fixture.KMP) {
                withoutAndroid()
                edit("build.gradle.kts") {
                    it.replace(
                        "    sourceSets {\n        commonMain {",
                        "    unresolvedDeferred = me.tbsten.analysiscodegen.gradle.DeferredSeverity.Warning\n" +
                            "    sourceSets {\n        named(\"iosMain\") {\n            processor(acg.testProcessors.native)\n            arg(\"tp.processors\", \"Defer\")\n        }\n        commonMain {",
                    )
                }
                write(
                    "src/commonMain/kotlin/fixture/kmp/Visibility.kt",
                    "package fixture.kmp\n@me.tbsten.analysiscodegen.tp.DeferUntil(\"java.io.File\")\nclass CommonWantsJava",
                )
                write(
                    "src/iosMain/kotlin/fixture/kmp/IosVisibility.kt",
                    """
                    package fixture.kmp
                    @me.tbsten.analysiscodegen.tp.DeferUntil("platform.UIKit.UIView")
                    class IosWantsUiKit
                    @me.tbsten.analysiscodegen.tp.DeferUntil("java.io.File")
                    class IosWantsJava
                    """,
                )
            }
        val result = p.build("analysisCodeGenCommonMain", "analysisCodeGenIosMain")
        p.generated("commonMain").kotlin("fixture/kmp/CommonWantsJavaResolved.kt").isFile shouldBe false
        p.generated("iosMain").kotlin("fixture/kmp/IosWantsUiKitResolved.kt").isFile shouldBe true
        p.generated("iosMain").kotlin("fixture/kmp/IosWantsJavaResolved.kt").isFile shouldBe false
        result.output shouldContain "CommonWantsJava"
        result.output shouldContain "IosWantsJava"
    }

    case("KMP-07", "commonMain の解析で共通 metadata 依存（kotlinx-coroutines-core の common 部分）が解決される", M1) {
        val p =
            fixtureProject(Fixture.KMP) {
                withoutAndroid()
                edit("build.gradle.kts") {
                    it.replace(
                        "        commonTest.dependencies {",
                        "        commonMain.dependencies {\n            implementation(\"org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2\")\n        }\n        commonTest.dependencies {",
                    )
                }
                write(
                    "src/commonMain/kotlin/fixture/kmp/Flows.kt",
                    """
                    package fixture.kmp
                    @me.tbsten.analysiscodegen.tp.Mapped
                    data class Flows(val flow: kotlinx.coroutines.flow.Flow<Int>, val job: kotlinx.coroutines.Job?)
                    """,
                )
            }
        p.build("analysisCodeGenCommonMain")
        val mapper = p.generated("commonMain").kotlin("fixture/kmp/FlowsMapper.kt").readText()
        mapper shouldContain "\"flow\" to \"kotlinx.coroutines.flow.Flow<kotlin.Int>\""
        mapper shouldContain "\"job\" to \"kotlinx.coroutines.Job?\""
    }

    case("KMP-08", "中間 SourceSet で commonize されたプラットフォームライブラリ（platform.posix など）が解決される", M2) {
        val p =
            fixtureProject(Fixture.KMP_CUSTOM) {
                edit("build.gradle.kts") {
                    it.replace(
                        "        named(\"sharedJvmJsMain\") {",
                        "        named(\"nativeMain\") {\n            processor(acg.testProcessors.native)\n            arg(\"tp.processors\", \"Defer\")\n        }\n        named(\"sharedJvmJsMain\") {",
                    )
                }
                write(
                    "src/nativeMain/kotlin/fixture/custom/Posix.kt",
                    "package fixture.custom\n@me.tbsten.analysiscodegen.tp.DeferUntil(\"platform.posix.stat\")\nclass NeedsPosix",
                )
            }
        p.build("analysisCodeGenNativeMain")
        p.generated("nativeMain").kotlin("fixture/custom/NeedsPosixResolved.kt").isFile shouldBe true
    }

    case("KMP-11", "commonMain の生成物を jvmMain の Processor が解析できる", M1) {
        val p =
            fixtureProject(Fixture.KMP) {
                withoutAndroid()
                edit("build.gradle.kts") {
                    it.replace(
                        "        commonMain {",
                        "        jvmMain {\n            processor(acg.testProcessors.native)\n            arg(\"tp.processors\", \"Defer\")\n        }\n        commonMain {",
                    )
                }
                // SharedMapper は commonMain の実行単位の生成物
                write(
                    "src/jvmMain/kotlin/fixture/kmp/NeedsCommonGenerated.kt",
                    "package fixture.kmp\n@me.tbsten.analysiscodegen.tp.DeferUntil(\"fixture.kmp.SharedMapper\")\nclass NeedsCommonGenerated",
                )
            }
        val planned = p.plannedTasks("analysisCodeGenJvmMain")
        planned.shouldContainInOrder(":analysisCodeGenCommonMain", ":analysisCodeGenJvmMain")
        p.build("analysisCodeGenJvmMain")
        p.generated("jvmMain").kotlin("fixture/kmp/NeedsCommonGeneratedResolved.kt").isFile shouldBe true
    }

    case("KMP-12", "1 つの SourceSet が複数の Compilation に属するとき、Compilation 単位の実行ではそれぞれのクラスパス・プラットフォームで解析される", M2) {
        val out =
            kotlin.io.path
                .createTempFile("probe", ".jsonl")
                .toFile()
                .apply { delete() }
        val p =
            fixtureProject(Fixture.KMP) {
                withoutAndroid()
                edit("build.gradle.kts") {
                    it.replace(
                        "arg(\"tp.processors\", \"Actual,Registry\")",
                        "arg(\"tp.processors\", \"Actual,Registry,Probe\")\n            arg(\"tp.probe.out\", \"${out.invariantSeparatorsPath}\")",
                    )
                }
            }
        p.build("analysisCodeGenJvmMainCompilation", "analysisCodeGenJsMainCompilation", "analysisCodeGenLinuxX64MainCompilation")
        val byUnit = readProbe(out).filter { it.event == "process" && it.round == 1 }.associateBy { it.unit }
        byUnit.getValue("jvmMain").platform shouldBe "jvm"
        byUnit.getValue("jsMain").platform shouldBe "js"
        byUnit.getValue("linuxX64Main").platform shouldBe "native"
        // commonMain は 3 つの Compilation に属し、どの Compilation からも参加 SourceSet として見える
        byUnit.values.forEach { it.extra.getValue("dependsOn") shouldContain "commonMain" }
    }

    case("KMP-13", "compilations.all の Processor がプラットフォームごとに別々の集約出力（DI Registry 相当）を作る", M1) {
        val p = fixtureProject(Fixture.KMP) { withoutAndroid() }
        p.build("jvmTest", "compileKotlinJs")
        val jvm = p.generatedForCompilation("jvmMain").kotlin("me/tbsten/analysiscodegen/tp/generated/TpRegistry.kt").readText()
        val js = p.generatedForCompilation("jsMain").kotlin("me/tbsten/analysiscodegen/tp/generated/TpRegistry.kt").readText()
        jvm shouldContain "\"fixture.kmp.JvmOnly\""
        jvm shouldNotContain "JsOnly"
        js shouldContain "\"fixture.kmp.JsOnly\""
        js shouldNotContain "JvmOnly"
    }

    case("KMP-14", "commonTest / jvmTest でも生成でき、main の生成物がテストから見える", M2) {
        val p =
            fixtureProject(Fixture.KMP) {
                withoutAndroid()
                edit("build.gradle.kts") {
                    it.replace(
                        "        commonMain {",
                        "        commonTest {\n            processor(acg.testProcessors.native)\n            arg(\"tp.processors\", \"Mapper\")\n        }\n        commonMain {",
                    )
                }
                write(
                    "src/commonTest/kotlin/fixture/kmp/TestModel.kt",
                    "package fixture.kmp\n@me.tbsten.analysiscodegen.tp.Mapped\ndata class TestModel(val s: Shared)\nval useMain = SharedMapper.propertyTypes",
                )
            }
        val result = p.build("jvmTest")
        result.task(":analysisCodeGenCommonTest")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":jvmTest")?.outcome shouldBe TaskOutcome.SUCCESS
        p.generated("commonTest").kotlin("fixture/kmp/TestModelMapper.kt").readText() shouldContain "fixture.kmp.Shared"
    }

    case("KMP-17", "デフォルト階層テンプレートと手で組んだ dependsOn 階層の両方で KMP-01〜05 が通る", M2) {
        // 手で組んだ階層（F-KMP-CUSTOM）: 中間 SourceSet の生成物を jvm と js から使い、actual は Compilation ごと
        val p = fixtureProject(Fixture.KMP_CUSTOM)
        val result = p.build("jvmTest", "compileKotlinJs")
        result.task(":analysisCodeGenSharedJvmJsMain")?.outcome shouldBe TaskOutcome.SUCCESS
        p.allGenerated().keys.filter { it.endsWith("JvmJsModelMapper.kt") } shouldBe
            listOf("sharedJvmJsMain/kotlin/fixture/custom/JvmJsModelMapper.kt")
        p.generatedForCompilation("jvmMain").kotlin("fixture/custom/CommonActual.kt").readText() shouldContain "\"jvm\""
        p.generatedForCompilation("jsMain").kotlin("fixture/custom/CommonActual.kt").readText() shouldContain "\"js\""
        // デフォルト階層側は KMP-01〜05 のテストそのもの（F-KMP）で確かめる
    }

    case("KMP-18", "全ターゲット（jvm, android, iosArm64, iosSimulatorArm64, macosArm64, linuxX64, mingwX64, js(IR), wasmJs）で生成とコンパイルが通る", M2) {
        // mac ランナーで動かす（Apple ターゲット）。mingwX64 はクロスコンパイル
        val p =
            fixtureProject(Fixture.KMP) {
                edit("build.gradle.kts") { it.replace("    linuxX64()", "    linuxX64()\n    macosArm64()\n    mingwX64()") }
            }
        val compiles =
            listOf(
                "Jvm",
                "IosArm64",
                "IosSimulatorArm64",
                "MacosArm64",
                "LinuxX64",
                "MingwX64",
                "Js",
                "WasmJs",
            ).map { ":compileKotlin$it" } +
                ":compileAndroidMain"
        val result = p.build(*compiles.toTypedArray())
        compiles.forEach { result.task(it)?.outcome shouldBe TaskOutcome.SUCCESS }
        listOf(
            "jvmMain",
            "androidMain",
            "iosArm64Main",
            "iosSimulatorArm64Main",
            "macosArm64Main",
            "linuxX64Main",
            "mingwX64Main",
            "jsMain",
            "wasmJsMain",
        ).forEach { p.generatedForCompilation(it).kotlin("fixture/kmp/SharedActual.kt").isFile shouldBe true }
    }

    case("KMP-19", "KMP の androidMain で Build Variant ごとに生成できる", M2) {
        // com.android.kotlin.multiplatform.library は Variant を 1 つ（main）にまとめる。F-KMP の Android Compilation に生成されることを見る
        val p = fixtureProject(Fixture.KMP)
        val result = p.build("compileAndroidMain")
        result.task(":analysisCodeGenAndroidMainCompilation")?.outcome shouldBe TaskOutcome.SUCCESS
        p.generatedForCompilation("androidMain").kotlin("fixture/kmp/SharedActual.kt").readText() shouldContain "\"jvm\""
        p.generatedForCompilation("androidMain").kotlin("me/tbsten/analysiscodegen/tp/generated/TpRegistry.kt").isFile shouldBe false
    }

    case("KMP-20", "KMP モジュール :shared を Android の :app が使い、:shared の commonMain 生成物が見える", M2) {
        val p =
            fixtureProject(Fixture.ANDROID) {
                // F-KMP を :shared として足す
                val kmp =
                    me.tbsten.analysiscodegen.e2e.support.E2EEnvironment.repoRoot
                        .resolve("fixtures/projects/kmp")
                kmp.resolve("src").copyRecursively(file("shared/src"))
                write(
                    "shared/build.gradle.kts",
                    kmp.resolve("build.gradle.kts").readText().replace("namespace = \"fixture.kmp\"", "namespace = \"fixture.kmp.shared\""),
                )
                edit("settings.gradle.kts") { it.replace("include(\":lib\", \":app\")", "include(\":lib\", \":app\", \":shared\")") }
                edit("app/build.gradle.kts") {
                    it.replace(
                        "implementation(project(\":lib\"))",
                        "implementation(project(\":lib\"))\n    implementation(project(\":shared\"))",
                    )
                }
                write(
                    "app/src/main/kotlin/fixture/android/app/UsesShared.kt",
                    "package fixture.android.app\nval shared = fixture.kmp.SharedMapper.toMap(fixture.kmp.Shared(1, \"a\", emptyList()))",
                )
            }
        val result = p.build(":app:assembleFreeDebug")
        result.task(":shared:analysisCodeGenCommonMain")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":app:compileFreeDebugKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    case("KMP-21", "iosMain だけに Processor を付けたとき、他の Compilation のタスクや出力に影響しない", M1) {
        val p =
            emptyProject {
                file("src/main").deleteRecursively()
                write(
                    "build.gradle.kts",
                    """
                    plugins {
                        alias(libs.plugins.kotlin.multiplatform)
                        id("me.tbsten.analysiscodegen")
                    }
                    kotlin {
                        jvm()
                        iosArm64()
                        iosSimulatorArm64()
                    }
                    analysisCodeGen {
                        arg("tp.processors", "Mapper")
                        sourceSets { named("iosMain") { processor(acg.testProcessors.native) } }
                    }
                    """,
                )
                write("src/commonMain/kotlin/me/tbsten/analysiscodegen/tp/TpAnnotations.kt", tpAnnotationsSource())
                write("src/commonMain/kotlin/c/Common.kt", "package c\n@me.tbsten.analysiscodegen.tp.Mapped\ndata class Common(val x: Int)")
            }
        val jvm = p.build("compileKotlinJvm")
        jvm.analysisCodegenTaskPaths().shouldBeEmpty()
        p.file("build/generated/analysisCodeGen").exists() shouldBe false
        val tasks =
            p
                .build("tasks", "--all")
                .output
                .lines()
                .filter { it.startsWith("analysisCodeGen") }
                .map { it.substringBefore(' ') }
        tasks shouldBe listOf("analysisCodeGenIosMain")
    }

    case("KMP-22", "Linux 上で Apple 向け SourceSet の解析ができる。できない構成では、原因が分かる診断が出る", M2) {
        // Linux ランナーで動かす。P-01（ADR-009）: Apple の KLIB は Linux で取得できる範囲だけ解析でき、platform.* が要る解析は診断を出す
        val p =
            fixtureProject(Fixture.KMP) {
                withoutAndroid()
                edit("build.gradle.kts") {
                    it.replace(
                        "    sourceSets {\n        commonMain {",
                        "    sourceSets {\n        named(\"iosMain\") {\n            processor(acg.testProcessors.native)\n            arg(\"tp.processors\", \"Mapper\")\n        }\n        commonMain {",
                    )
                }
                write(
                    "src/iosMain/kotlin/fixture/kmp/IosModel.kt",
                    "package fixture.kmp\n@me.tbsten.analysiscodegen.tp.Mapped\ndata class IosModel(val x: Int)",
                )
                write(
                    "src/iosMain/kotlin/fixture/kmp/UsesUiKit.kt",
                    "package fixture.kmp\n@me.tbsten.analysiscodegen.tp.Mapped\ndata class UsesUiKit(val view: platform.UIKit.UIView?)",
                )
            }
        val result = p.runner("analysisCodeGenIosMain").run()
        if (result.task(":analysisCodeGenIosMain")?.outcome == TaskOutcome.SUCCESS) {
            p.generated("iosMain").kotlin("fixture/kmp/IosModelMapper.kt").isFile shouldBe true
            p.generated("iosMain").kotlin("fixture/kmp/UsesUiKitMapper.kt").readText() shouldContain "platform.UIKit.UIView?"
        } else {
            result.output shouldContain "iosMain"
            result.output shouldContain "macOS"
            result.output shouldNotContain "NotImplementedError"
        }
    }

    case("KMP-24", "KSP の Issue #965 / #2621 の再現シナリオで、共通の生成物が 1 回だけ作られ下流から見える", M1) {
        // #965: commonMain の生成物が jvmMain / jsMain から見えない、#2621: ターゲットごとに重複して生成され redeclaration になる
        val p =
            emptyProject {
                file("src/main").deleteRecursively()
                write(
                    "build.gradle.kts",
                    """
                    plugins {
                        alias(libs.plugins.kotlin.multiplatform)
                        id("me.tbsten.analysiscodegen")
                    }
                    kotlin {
                        jvm()
                        js { nodejs() }
                        linuxX64()
                    }
                    analysisCodeGen {
                        arg("tp.processors", "Mapper")
                        sourceSets { commonMain { processor(acg.testProcessors.native) } }
                    }
                    """,
                )
                write("src/commonMain/kotlin/me/tbsten/analysiscodegen/tp/TpAnnotations.kt", tpAnnotationsSource())
                write(
                    "src/commonMain/kotlin/c/Model.kt",
                    "package c\n@me.tbsten.analysiscodegen.tp.Mapped\ndata class Model(val x: Int)\nval fromCommon = ModelMapper.propertyTypes",
                )
                for (t in listOf("jvm", "js", "linuxX64")) {
                    write("src/${t}Main/kotlin/c/Use.kt", "package c\nval fromTarget = ModelMapper.toMap(Model(1))")
                }
            }
        val result = p.build("compileKotlinJvm", "compileKotlinJs", "compileKotlinLinuxX64")
        result.output shouldNotContain "Redeclaration"
        p.allGenerated().keys.filter { it.endsWith("ModelMapper.kt") } shouldBe listOf("commonMain/kotlin/c/ModelMapper.kt")
    }

    case("KMP-25", "IDE のインポートで、KMP の各 SourceSet に生成ディレクトリが登録される", M2) {
        // IDE の Gradle 同期は prepareKotlinIdeaImport を実行し、SourceSet の srcDirs を読む
        val p =
            fixtureProject(Fixture.KMP) {
                withoutAndroid()
                edit("build.gradle.kts") {
                    it +
                        """

                        tasks.register("printKmpSrcDirs") {
                            val dirs = kotlin.sourceSets.associate { s -> s.name to s.kotlin.srcDirs.map { d -> d.invariantSeparatorsPath } }
                            doLast { dirs.forEach { (n, d) -> d.forEach { println("SRC " + n + "=" + it) } } }
                        }
                        """.trimIndent()
                }
            }
        val result = p.build("prepareKotlinIdeaImport", "printKmpSrcDirs")
        result.task(":analysisCodeGenCommonMain")?.outcome shouldBe TaskOutcome.SUCCESS
        val lines = result.output.lines().filter { it.startsWith("SRC ") }
        lines shouldContain "SRC commonMain=${p.dir.invariantSeparatorsPath}/build/generated/analysisCodeGen/commonMain/kotlin"
        lines.none { it.startsWith("SRC jvmMain=") && it.contains("analysisCodeGen/commonMain") } shouldBe true
        lines.any { it.startsWith("SRC jvmMain=") && it.endsWith("analysisCodeGen/compilations/jvmMain/kotlin") } shouldBe true
        lines.shouldNotContain("SRC jvmTest=${p.dir.invariantSeparatorsPath}/build/generated/analysisCodeGen/compilations/jvmMain/kotlin")
    }
})

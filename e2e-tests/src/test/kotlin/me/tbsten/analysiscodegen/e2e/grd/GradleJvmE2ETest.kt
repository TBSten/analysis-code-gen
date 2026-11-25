package me.tbsten.analysiscodegen.e2e.grd

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import me.tbsten.analysiscodegen.e2e.support.Fixture
import me.tbsten.analysiscodegen.e2e.support.emptyProject
import me.tbsten.analysiscodegen.e2e.support.fixtureProject
import me.tbsten.analysiscodegen.e2e.support.plannedTasks
import me.tbsten.analysiscodegen.e2e.support.processorModule
import me.tbsten.analysiscodegen.e2e.support.readProbe
import me.tbsten.analysiscodegen.testfoundation.E2ESpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import org.gradle.testkit.runner.TaskOutcome

private val M1 = PendingCase(Milestone.M1)
private val M2 = PendingCase(Milestone.M2)

// 5.1 Gradle / KGP 統合（GRD）のうち JVM・Java・マルチモジュール・DSL・Worker のケース。Android は GradleAndroidE2ETest
class GradleJvmE2ETest : E2ESpec({
    case("GRD-01", "JVM 単一モジュールに plugin を適用すると analysisCodeGenMain / analysisCodeGenTest が登録され、生成物込みで build が成功する", M1) {
        val p = fixtureProject(Fixture.JVM)
        p.build("tasks", "--all").output.let {
            it shouldContain "analysisCodeGenMain"
            it shouldContain "analysisCodeGenTest"
        }
        val result = p.build("build")
        result.task(":analysisCodeGenMain")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":analysisCodeGenTest")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":compileKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":test")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    case("GRD-02", "生成物が build/generated/analysisCodeGen/<SourceSet>/{kotlin,java,resources} に出力され、他のタスクと同じディレクトリに書かない", M1) {
        val p = fixtureProject(Fixture.JVM)
        p.build("analysisCodeGenMain")
        val main = p.generated("main")
        main.kotlin("fixture/jvm/UserMapper.kt").isFile shouldBe true
        main.java("fixture/jvm/ReportJava.java").isFile shouldBe true
        main.resources("META-INF/services/me.tbsten.analysiscodegen.tp.TpResourceMarker").isFile shouldBe true
        // 出力ディレクトリを他のタスクと共有すると Gradle が overlapping outputs を警告する
        p.build("build", "--warning-mode=all").output shouldNotContain "overlapping outputs"
        p
            .file("build/generated/analysisCodeGen")
            .listFiles()!!
            .map { it.name }
            .sorted() shouldBe listOf("main", "test")
    }

    case("GRD-03", "Task Graph 上で compile タスクが analysisCodeGen タスクに依存し、自タスクの出力が自タスクの入力に入らない（循環なし）", M1) {
        val p = fixtureProject(Fixture.JVM)
        val planned = p.plannedTasks("compileTestKotlin")
        planned.shouldContainInOrder(":analysisCodeGenMain", ":compileKotlin")
        planned.shouldContainInOrder(":analysisCodeGenTest", ":compileTestKotlin")
        // 2 回目は入力が変わらないので UP-TO-DATE（自分の出力が入力に入っていれば毎回動く）
        p.build("analysisCodeGenMain")
        p.build("analysisCodeGenMain").task(":analysisCodeGenMain")?.outcome shouldBe TaskOutcome.UP_TO_DATE
    }

    case("GRD-04", "生成ディレクトリが IDE 向けのソースルートにも登録される（Tooling API で取得したモデルに含まれる）", M2) {
        val p = fixtureProject(Fixture.JVM)
        val connector =
            org.gradle.tooling.GradleConnector
                .newConnector()
                .forProjectDirectory(p.dir)
        connector.connect().use { connection ->
            val idea =
                connection
                    .model(org.gradle.tooling.model.idea.IdeaProject::class.java)
                    .withArguments("-Pacg.repoRoot=${me.tbsten.analysiscodegen.e2e.support.E2EEnvironment.repoRoot}")
                    .get()
            val roots =
                idea.modules
                    .flatMap { m -> m.contentRoots.flatMap { r -> r.sourceDirectories.toList() } }
                    .map { it.directory.invariantSeparatorsPath }
            roots.any { it.endsWith("build/generated/analysisCodeGen/main/kotlin") } shouldBe true
            idea.modules
                .flatMap { m -> m.contentRoots.flatMap { r -> r.sourceDirectories.filter { d -> d.isGenerated } } }
                .any { it.directory.invariantSeparatorsPath.endsWith("build/generated/analysisCodeGen/main/kotlin") } shouldBe true
        }
    }

    case("GRD-05", "AA に渡すライブラリ集合が、その SourceSet / Compilation のコンパイルクラスパスと一致する", M1) {
        val p =
            fixtureProject(Fixture.JVM) {
                edit("build.gradle.kts") {
                    it +
                        """

                        dependencies {
                            compileOnly("org.jetbrains:annotations:26.0.2")
                            runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                        }

                        tasks.register("printClasspaths") {
                            val task = tasks.named<me.tbsten.analysiscodegen.gradle.AnalysisCodegenTask>("analysisCodeGenMain")
                            val acg = task.flatMap { t -> provider { t.compileClasspath.files.map { f -> f.name }.sorted() } }
                            val compile = configurations.compileClasspath.map { c -> c.files.map { f -> f.name }.sorted() }
                            doLast {
                                println("ACG=" + acg.get().joinToString(","))
                                println("COMPILE=" + compile.get().joinToString(","))
                            }
                        }
                        """.trimIndent()
                }
            }
        val out = p.build("printClasspaths").output.lines()
        val acg = out.first { it.startsWith("ACG=") }.removePrefix("ACG=").split(',')
        val compile = out.first { it.startsWith("COMPILE=") }.removePrefix("COMPILE=").split(',')
        acg shouldBe compile
        acg.any { it.startsWith("annotations-26") } shouldBe true
        acg.none { it.startsWith("kotlinx-coroutines-core") } shouldBe true
    }

    case("GRD-06", "languageVersion / apiVersion / opt-in / -X 言語機能フラグ / explicitApi が解析に反映される（context parameters を使うソースが解析エラーにならない）", M2) {
        val p =
            fixtureProject(Fixture.JVM) {
                edit("build.gradle.kts") {
                    it +
                        """

                        kotlin {
                            explicitApi()
                            compilerOptions {
                                freeCompilerArgs.add("-Xcontext-parameters")
                                optIn.add("kotlin.ExperimentalStdlibApi")
                            }
                        }
                        """.trimIndent()
                }
                // explicitApi のため既存のソースは使わず、context parameters を使う最小のソースに入れ替える
                file("src/main/kotlin/fixture").deleteRecursively()
                file("src/test").deleteRecursively()
                write(
                    "src/main/kotlin/ctx/Ctx.kt",
                    """
                    package ctx

                    import me.tbsten.analysiscodegen.tp.Mapped

                    public class Logger { public fun log(s: String): Unit = println(s) }

                    context(logger: Logger)
                    public fun hello(): Unit = logger.log("hi")

                    @Mapped
                    public data class Uses(public val hex: String = 1.toHexString())
                    """,
                )
            }
        val result = p.build("compileKotlin")
        result.output shouldNotContain "Unresolved reference"
        p.generated("main").kotlin("ctx/UsesMapper.kt").readText() shouldContain "\"hex\" to \"kotlin.String\""
    }

    case("GRD-07", "JDK Toolchain の指定（17 / 21）に従って JDK の API が解決される（17 指定時に 21 専用 API は未解決）", M2) {
        fun project(jdk: Int) =
            emptyProject {
                write(
                    "build.gradle.kts",
                    """
                    plugins {
                        alias(libs.plugins.kotlin.jvm)
                        id("me.tbsten.analysiscodegen")
                    }
                    kotlin { jvmToolchain($jdk) }
                    analysisCodeGen {
                        arg("tp.processors", "Defer")
                        sourceSets { main { processor(acg.testProcessors.native) } }
                    }
                    """,
                )
                // java.lang.MatchException は JDK 21 で追加された（17 には無い）
                write(
                    "src/main/kotlin/jdk/NeedsJdk21.kt",
                    """
                    package jdk
                    @me.tbsten.analysiscodegen.tp.DeferUntil("java.lang.MatchException")
                    class NeedsJdk21
                    """,
                )
            }
        project(21).build("analysisCodeGenMain").output shouldNotContain "NeedsJdk21"
        val p17 = project(17)
        val r17 = p17.buildAndFail("analysisCodeGenMain")
        r17.output shouldContain "java.lang.MatchException"
        p17.generated("main").kotlin("jdk/NeedsJdk21Resolved.kt").isFile shouldBe false
    }

    case("GRD-08", "Kotlin + Java 混在で、Kotlin から参照する Java クラスが解析で解決される", M1) {
        val p = fixtureProject(Fixture.JAVA)
        p.build("build")
        val mapper = p.generated("main").kotlin("fixture/java/HolderMapper.kt").readText()
        mapper shouldContain "\"person\" to \"fixture.java.JavaPerson\""
        mapper shouldContain "\"people\" to \"kotlin.collections.List<fixture.java.JavaPerson>\""
        // 手書きの Java クラスはラウンド 1 から解決できる（defer が残らない）
        p.generated("main").kotlin("fixture/java/NeedsJavaResolved.kt").isFile shouldBe true
    }

    case("GRD-09", "マルチモジュールで :app の Processor が :lib の公開宣言を解析できる", M1) {
        val p = fixtureProject(Fixture.JVM_MULTI)
        p.build(":app:analysisCodeGenMain")
        p.generated("main", "app").kotlin("fixture/multi/app/AppUserMapper.kt").readText() shouldContain
            "\"lib\" to \"fixture.multi.lib.LibUser\""
    }

    case("GRD-10", ":lib の生成物（コンパイル済み）が :app から参照できる", M1) {
        val p = fixtureProject(Fixture.JVM_MULTI)
        val result = p.build(":app:build")
        result.task(":app:compileKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
        // LibUserMapper（:lib の生成物）が :app のラウンド 1 で解決されている
        p.generated("main", "app").kotlin("fixture/multi/app/NeedsLibMapperResolved.kt").isFile shouldBe true
    }

    case("GRD-16", "JVM の test SourceSet 向けに別タスクが走り、main の生成物が test から見える", M1) {
        val p =
            fixtureProject(Fixture.JVM) {
                write(
                    "src/test/kotlin/fixture/jvm/UsesMainGenerated.kt",
                    """
                    package fixture.jvm

                    import kotlin.test.Test
                    import kotlin.test.assertEquals

                    class UsesMainGenerated {
                        @Test fun mainMapperIsVisible() = assertEquals("alice", UserMapper.toMap(User(1, "alice"))["name"])
                    }
                    """,
                )
            }
        val result = p.build("test")
        result.task(":analysisCodeGenTest")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":test")?.outcome shouldBe TaskOutcome.SUCCESS
        // main の生成物を test の実行単位が作り直していない
        p.generated("test").kotlin("fixture/jvm/UserMapper.kt").isFile shouldBe false
    }

    case("GRD-17", "Processor を analysisCodeGen(...) 依存と SourceSet DSL の processor(...) のどちらで指定しても解決・実行される", M1) {
        fun project(useDependencies: Boolean) =
            fixtureProject(Fixture.JVM) {
                edit("build.gradle.kts") {
                    if (useDependencies) {
                        it.replace("main { processor(acg.testProcessors.native) }", "") +
                            "\n\ndependencies { analysisCodeGen(acg.testProcessors.native) }\n"
                    } else {
                        it
                    }
                }
            }
        val viaDsl = project(useDependencies = false).also { it.build("analysisCodeGenMain") }
        val viaDeps = project(useDependencies = true).also { it.build("analysisCodeGenMain") }
        viaDsl
            .generated("main")
            .files()
            .keys
            .shouldContain("kotlin/fixture/jvm/UserMapper.kt")
        viaDeps.generated("main").files() shouldBe viaDsl.generated("main").files()
    }

    case("GRD-18", "sourceSets.all / compilations.all の一括登録が効き、個別指定と重なったときの優先順位が ADR どおり", M2) {
        // ADR-016: Processor は all と個別の和集合で、同じ座標は 1 回だけ。同じ実行単位で同じキーに違う値が 2 回なら設定エラー
        val p =
            fixtureProject(Fixture.JVM) {
                edit("build.gradle.kts") {
                    it.replace(
                        "sourceSets {",
                        """
                        sourceSets {
                            all { processor(acg.testProcessors.native) }
                        """.trimIndent(),
                    )
                }
            }
        val out = p.dir.resolve("probe.jsonl")
        p.edit("build.gradle.kts") {
            it.replace("\"Mapper,Registry,Chain,Body,Defer,Resource\"", "\"Mapper,Probe\"") +
                "\nanalysisCodeGen { arg(\"tp.probe.out\", \"${out.invariantSeparatorsPath}\") }\n"
        }
        p.build("analysisCodeGenMain")
        // all と main の両方に同じ座標を書いても create は 1 回
        readProbe(out).count { it.event == "create" && it.unit == "main" } shouldBe 1

        val conflict =
            fixtureProject(Fixture.JVM) {
                edit("build.gradle.kts") {
                    it.replace(
                        "sourceSets {",
                        """
                        sourceSets {
                            all { arg("k", "a") }
                            main { arg("k", "b") }
                        """.trimIndent(),
                    )
                }
            }
        conflict.buildAndFail("analysisCodeGenMain").output shouldContain "k"
    }

    case("GRD-19", "Processor オプションのグローバル指定と SourceSet スコープ指定が渡り、スコープ側が優先される", M1) {
        val out =
            kotlin.io.path
                .createTempFile("probe", ".jsonl")
                .toFile()
                .apply { delete() }
        val p =
            fixtureProject(Fixture.JVM) {
                write(
                    "build.gradle.kts",
                    """
                    plugins {
                        alias(libs.plugins.kotlin.jvm)
                        id("me.tbsten.analysiscodegen")
                    }
                    analysisCodeGen {
                        arg("tp.processors", "Probe")
                        arg("tp.probe.out", "${out.invariantSeparatorsPath}")
                        arg("global.only", "g")
                        arg("both", "global")
                        sourceSets {
                            main {
                                processor(acg.testProcessors.native)
                                arg("both", "scoped")
                            }
                        }
                    }
                    """,
                )
                file("src/test").deleteRecursively()
            }
        p.build("analysisCodeGenMain")
        val options = readProbe(out).first { it.event == "create" }.options
        options["global.only"] shouldBe "g"
        options["both"] shouldBe "scoped"
    }

    case("GRD-20", "Processor が別バージョンの依存（例: Guava、kotlinx-serialization）を持っても、ビルドや AA と衝突しない（ClassLoader 分離）", M2) {
        // Processor 側に古い Guava を、ビルドスクリプト側に新しい Guava を載せる。どちらも自分の版を見る
        val p =
            emptyProject(includes = listOf(":processor")) {
                processorModule(
                    "processor",
                    "p.GuavaProvider",
                    """
                    package p

                    import me.tbsten.analysiscodegen.processor.*

                    class GuavaProvider : AnalysisCodegenProcessorProvider {
                        override fun create(environment: AnalysisCodegenProcessorEnvironment) =
                            object : AnalysisCodegenProcessor {
                                override fun process(context: AnalysisCodegenRoundContext) {
                                    if (context.round != 1) return
                                    // Processor の ClassLoader から見える Guava の版を出力する
                                    val version = com.google.common.base.Strings::class.java.`package`.implementationVersion ?: "unknown"
                                    context.codeGenerator.generateKotlin("gen", "GuavaVersion", Dependencies.all, "package gen\nconst val GUAVA = \"${'$'}version\"\n")
                                }
                            }
                    }
                    """,
                    dependencies = listOf("implementation(\"com.google.guava:guava:20.0\")"),
                )
                write(
                    "build.gradle.kts",
                    """
                    buildscript { dependencies { classpath("com.google.guava:guava:33.4.0-jre") } }
                    plugins {
                        alias(libs.plugins.kotlin.jvm)
                        id("me.tbsten.analysiscodegen")
                    }
                    analysisCodeGen { sourceSets { main { processor(project(":processor")) } } }
                    tasks.register("printGuava") {
                        val v = com.google.common.base.Strings::class.java.`package`.implementationVersion ?: "unknown"
                        doLast { println("BUILDSCRIPT_GUAVA=" + v) }
                    }
                    """,
                )
                write("src/main/kotlin/a/A.kt", "package a\nclass A")
            }
        val result = p.build("analysisCodeGenMain", "printGuava")
        p.generated("main").kotlin("gen/GuavaVersion.kt").readText() shouldContain "\"20.0\""
        result.output shouldContain "BUILDSCRIPT_GUAVA=33.4.0-jre"
    }

    case("GRD-21", "AA / IntelliJ Platform がビルドスクリプトの classpath に載らない（Worker 側だけで読み込まれる）", M2) {
        val p =
            fixtureProject(Fixture.JVM) {
                edit("build.gradle.kts") {
                    it +
                        """

                        tasks.register("probeBuildscriptClasspath") {
                            val loader = me.tbsten.analysiscodegen.gradle.AnalysisCodegenPlugin::class.java.classLoader
                            val visible =
                                listOf(
                                    "org.jetbrains.kotlin.analysis.api.KaSession",
                                    "com.intellij.openapi.project.Project",
                                    "me.tbsten.analysiscodegen.engine.AnalysisCodegenEngine",
                                ).filter { name -> runCatching { Class.forName(name, false, loader) }.isSuccess }
                            doLast { println("VISIBLE=" + visible.joinToString(",")) }
                        }
                        """.trimIndent()
                }
            }
        val result = p.build("probeBuildscriptClasspath", "analysisCodeGenMain")
        result.output.lines().first { it.startsWith("VISIBLE=") } shouldBe "VISIBLE="
        result.task(":analysisCodeGenMain")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    case("GRD-22", "遅延設定: help だけを実行したとき analysisCodeGen タスクが realize されない", M2) {
        val p =
            fixtureProject(Fixture.JVM) {
                edit("build.gradle.kts") {
                    it +
                        """

                        tasks.withType<me.tbsten.analysiscodegen.gradle.AnalysisCodegenTask>().configureEach {
                            println("REALIZED " + name)
                        }
                        """.trimIndent()
                }
            }
        p.build("help").output shouldNotContain "REALIZED analysisCodeGen"
        p.build("analysisCodeGenMain").output shouldContain "REALIZED analysisCodeGenMain"
    }

    case("GRD-23", "同じモジュールで KSP と併用でき、タスクの循環が起きず、生成物の可視性が ADR で決めた規則どおり", M2) {
        // 決め打ち（ADR-038）: KSP と analysisCodeGen は互いの生成物を入力にしない。どちらの生成物も compileKotlin からは見える
        val p =
            fixtureProject(Fixture.JVM) {
                edit("build.gradle.kts") {
                    it
                        .replace("id(\"me.tbsten.analysiscodegen\")", "id(\"me.tbsten.analysiscodegen\")\n    alias(libs.plugins.ksp)")
                        .replace("\"Mapper,Registry,Chain,Body,Defer,Resource\"", "\"Mapper\"") +
                        """

                        dependencies { ksp(acg.testProcessors.ksp) }
                        ksp { arg("tp.processors", "Registry") }
                        """.trimIndent()
                }
            }
        val planned = p.plannedTasks("compileKotlin")
        planned.shouldContainAll(":analysisCodeGenMain", ":kspKotlin", ":compileKotlin")
        val result = p.build("build")
        result.task(":compileKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
        p.generated("main").kotlin("fixture/jvm/UserMapper.kt").isFile shouldBe true
        p
            .generated("main")
            .files()
            .keys
            .none { it.contains("TpRegistry") } shouldBe true
        p.file("build/generated/ksp/main/kotlin/me/tbsten/analysiscodegen/tp/generated/TpRegistry.kt").isFile shouldBe true
    }

    case("GRD-24", "対応表の最小と最新の Gradle でビルドが通る", M2) {
        // 対応表（ADR-011・docs のインストール）: 最小 9.0、最新は wrapper の版
        for (gradle in listOf(
            "9.0.0",
            org.gradle.util.GradleVersion
                .current()
                .version,
        )) {
            val p = fixtureProject(Fixture.JVM) { gradleVersion = gradle }
            p.build("build").task(":analysisCodeGenMain")?.outcome shouldBe TaskOutcome.SUCCESS
        }
    }

    case("GRD-25", "--parallel のマルチモジュールで同時実行しても結果が壊れない", M2) {
        val serial = fixtureProject(Fixture.JVM_MULTI).also { it.build("build", "--no-parallel") }
        val parallel = fixtureProject(Fixture.JVM_MULTI).also { it.build("build", "--parallel", "--max-workers=4") }
        for (module in listOf("lib", "app")) {
            parallel.allGenerated(module) shouldBe serial.allGenerated(module)
        }
    }

    case("GRD-26", "Composite build（includeBuild）で開発中の Processor を使える", M2) {
        val p =
            emptyProject {
                write(
                    "../my-processor/settings.gradle.kts",
                    """
                    pluginManagement { includeBuild("../build-support") }
                    plugins { id("acg.fixture-settings") }
                    rootProject.name = "my-processor"
                    """,
                )
                processorModule(
                    "../my-processor",
                    "p.HelloProvider",
                    """
                    package p
                    import me.tbsten.analysiscodegen.processor.*
                    class HelloProvider : AnalysisCodegenProcessorProvider {
                        override fun create(environment: AnalysisCodegenProcessorEnvironment) =
                            object : AnalysisCodegenProcessor {
                                override fun process(context: AnalysisCodegenRoundContext) {
                                    if (context.round == 1) context.codeGenerator.generateKotlin("gen", "Hello", Dependencies.all, "package gen\nobject Hello")
                                }
                            }
                    }
                    """,
                )
                edit("settings.gradle.kts") { it + "\nincludeBuild(\"../my-processor\")\n" }
                write(
                    "build.gradle.kts",
                    """
                    plugins {
                        alias(libs.plugins.kotlin.jvm)
                        id("me.tbsten.analysiscodegen")
                    }
                    analysisCodeGen { sourceSets { main { processor("example:my-processor:1.0") } } }
                    """,
                )
                write("src/main/kotlin/a/UsesHello.kt", "package a\nval h = gen.Hello")
            }
        val result = p.build("build")
        result.task(":my-processor:jar")?.outcome shouldBe TaskOutcome.SUCCESS
        p.generated("main").kotlin("gen/Hello.kt").isFile shouldBe true
    }

    case("GRD-27", "Processor を指定していない SourceSet ではタスクが実行されない（登録しないか NO-SOURCE）", M1) {
        val p =
            fixtureProject(Fixture.JVM) {
                edit("build.gradle.kts") { it.replace("test { processor(acg.testProcessors.native) }", "") }
            }
        val result = p.build("build")
        result.task(":analysisCodeGenMain")?.outcome shouldBe TaskOutcome.SUCCESS
        val testTask = result.task(":analysisCodeGenTest")
        (testTask == null || testTask.outcome == TaskOutcome.NO_SOURCE) shouldBe true
        p.generated("test").root.exists() shouldBe false
    }

    case("GRD-28", "Kotlin plugin が無いプロジェクトに適用したら、原因が分かるエラーで失敗する", M1) {
        val p =
            emptyProject {
                write("build.gradle.kts", "plugins { java\n id(\"me.tbsten.analysiscodegen\") }")
            }
        val result = p.buildAndFail("help")
        result.output shouldContain "kotlin(\"jvm\")"
        result.output shouldContain "kotlin(\"multiplatform\")"
        result.output shouldNotContain "NotImplementedError"
    }

    case("GRD-29", "Worker のヒープ上限など実行設定を DSL で変えられ、反映される", M2) {
        val p =
            emptyProject(includes = listOf(":processor")) {
                processorModule(
                    "processor",
                    "p.JvmInfoProvider",
                    """
                    package p
                    import me.tbsten.analysiscodegen.processor.*
                    // Processor が動いている JVM の設定を生成物に書き出す
                    class JvmInfoProvider : AnalysisCodegenProcessorProvider {
                        override fun create(environment: AnalysisCodegenProcessorEnvironment) =
                            object : AnalysisCodegenProcessor {
                                override fun process(context: AnalysisCodegenRoundContext) {
                                    if (context.round != 1) return
                                    val max = Runtime.getRuntime().maxMemory()
                                    val marker = System.getProperty("acg.e2e.marker") ?: "none"
                                    context.codeGenerator.generateKotlin("gen", "JvmInfo", Dependencies.all, "package gen\nconst val MAX = ${'$'}{max}L\nconst val MARKER = \"${'$'}marker\"\n")
                                }
                            }
                    }
                    """,
                )
                write(
                    "build.gradle.kts",
                    """
                    plugins {
                        alias(libs.plugins.kotlin.jvm)
                        id("me.tbsten.analysiscodegen")
                    }
                    analysisCodeGen {
                        sourceSets { main { processor(project(":processor")) } }
                        worker {
                            isolation = me.tbsten.analysiscodegen.gradle.WorkerIsolation.Process
                            maxHeapSize = "777m"
                            jvmArgs.add("-Dacg.e2e.marker=grd29")
                        }
                    }
                    """,
                )
                write("src/main/kotlin/a/A.kt", "package a\nclass A")
            }
        p.build("analysisCodeGenMain")
        val info = p.generated("main").kotlin("gen/JvmInfo.kt").readText()
        info shouldContain "MARKER = \"grd29\""
        // maxMemory は -Xmx より少し小さくなるので上限と下限で見る
        val max = Regex("MAX = (\\d+)L").find(info)!!.groupValues[1].toLong()
        (max <= 777L * 1024 * 1024 && max > 512L * 1024 * 1024) shouldBe true
    }

    case("GRD-30", "Windows 上で F-JVM がビルドでき、生成物のパスと内容が Linux と一致する", PendingCase(Milestone.M3)) {
        // Windows ランナーで実行し、Linux の CI が残した期待値（全生成物のパスと中身）と比べる
        val p = fixtureProject(Fixture.JVM)
        p.build("build")
        val expectedDir =
            System.getProperty("acg.e2e.linuxGolden")?.let { java.io.File(it) } ?: error("Linux の生成物の場所 acg.e2e.linuxGolden が要る")
        val expected =
            expectedDir.walkTopDown().filter { it.isFile }.associate {
                it.relativeTo(expectedDir).invariantSeparatorsPath to
                    it.readText()
            }
        p.allGenerated().mapValues { it.value.replace("\r\n", "\n") } shouldBe expected
        p.allGenerated().values.none { it.contains("\r\n") } shouldBe true
    }
})

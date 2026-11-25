package me.tbsten.analysiscodegen.e2e.inc

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import me.tbsten.analysiscodegen.e2e.support.Acg45Engine
import me.tbsten.analysiscodegen.e2e.support.Acg45Repo
import me.tbsten.analysiscodegen.e2e.support.acg45Project
import me.tbsten.analysiscodegen.e2e.support.codegenTask
import me.tbsten.analysiscodegen.e2e.support.generatedFiles
import me.tbsten.analysiscodegen.e2e.support.generatedNames
import me.tbsten.analysiscodegen.e2e.support.mappedClass
import me.tbsten.analysiscodegen.e2e.support.probeArray
import me.tbsten.analysiscodegen.e2e.support.probeValue
import me.tbsten.analysiscodegen.e2e.support.readProbe
import me.tbsten.analysiscodegen.testfoundation.E2ESpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import me.tbsten.analysiscodegen.testing.gradle.GradleFixture
import me.tbsten.analysiscodegen.testprocessors.TpAnnotationsSource
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files

// 5.10 増分処理（INC）の E2E。最重要の不変条件は FullBuild(最終入力) == IncrementalBuild(変更履歴, 最終入力)。
// 「再処理されたか」は TP-Probe の記録（ラウンド 1 の newFiles = 今回処理したファイル）で見る（ADR-027・034）
private val native = Acg45Engine.Native
private val task = codegenTask(native)

private fun probeFile(): File = Files.createTempFile("acg-probe", ".jsonl").toFile().apply { delete() }

/** 最後の実行（最後の create 以降）のラウンド 1 で処理したファイル名。 */
private fun lastRound1Files(probe: File): Set<String> {
    val lines = readProbe(probe)
    val lastCreate = lines.indexOfLast { probeValue(it, "event") == "create" }
    return lines
        .drop(lastCreate + 1)
        .first { probeValue(it, "event") == "process" && probeValue(it, "round") == "1" }
        .let { probeArray(it, "newFiles") }
        .map { it.substringAfterLast('/') }
        .filterNot { it == "TpAnnotations.kt" }
        .toSet()
}

/** 増分で辿り着いた生成物と、同じ最終入力をクリーンに作った生成物が一致すること。 */
private fun shouldMatchCleanBuild(
    incremental: GradleFixture,
    processors: String = "Mapper,Registry",
) {
    val clean = acg45Project(processors = processors)
    incremental.projectDir.resolve("src").copyRecursively(clean.projectDir.resolve("src"), overwrite = true)
    clean.build(task)
    incremental.generatedFiles(native) shouldBe clean.generatedFiles(native)
}

class IncrementalE2ETest : E2ESpec({
    case("INC-01", "Level 0: --rerun-tasks で全件処理し、通常ビルドと同じ生成物になる", pending = PendingCase(Milestone.M1)) {
        val fixture =
            acg45Project {
                kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A"))
                kotlin("src/main/kotlin/com/example/B.kt", mappedClass("B"))
            }
        fixture.build(task).task(task)?.outcome shouldBe TaskOutcome.SUCCESS
        val normal = fixture.generatedFiles(native)
        normal.keys.map { it.substringAfterLast('/') } shouldContainAll listOf("AMapper.kt", "BMapper.kt", "TpRegistry.kt")

        fixture.build(task, "--rerun-tasks").task(task)?.outcome shouldBe TaskOutcome.SUCCESS
        fixture.generatedFiles(native) shouldBe normal
    }

    case("INC-02", "Level 1: 入力が変わらなければタスクが UP-TO-DATE", pending = PendingCase(Milestone.M1)) {
        val fixture = acg45Project { kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A")) }
        fixture.build(task).task(task)?.outcome shouldBe TaskOutcome.SUCCESS
        fixture.build(task).task(task)?.outcome shouldBe TaskOutcome.UP_TO_DATE
    }

    case("INC-03", "Level 2 isolating: A.kt を変えると AImpl だけ再生成し、BImpl は再利用する（処理ログで確認）", pending = PendingCase(Milestone.M2)) {
        val probe = probeFile()
        val fixture =
            acg45Project(processors = "Mapper,Probe", probeOut = probe) {
                kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A"))
                kotlin("src/main/kotlin/com/example/B.kt", mappedClass("B"))
            }
        fixture.build(task)
        lastRound1Files(probe) shouldContainExactlyInAnyOrder setOf("A.kt", "B.kt")

        fixture.kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A", props = "val id: Int, val name: String"))
        fixture.build(task)
        lastRound1Files(probe) shouldBe setOf("A.kt")
        val generated = fixture.generatedFiles(native)
        generated.entries.single { it.key.endsWith("AMapper.kt") }.value shouldContain "name"
        generated.keys.map { it.substringAfterLast('/') } shouldContain "BMapper.kt"
        shouldMatchCleanBuild(fixture, "Mapper,Probe")
    }

    case("INC-04", "集約出力: ファイルを追加すると Registry が再生成される", pending = PendingCase(Milestone.M2)) {
        val fixture = acg45Project { kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A")) }
        fixture.build(task)
        val registry = {
            fixture
                .generatedFiles(native)
                .entries
                .single { it.key.endsWith("TpRegistry.kt") }
                .value
        }
        registry() shouldNotContain "com.example.B"

        fixture.kotlin("src/main/kotlin/com/example/B.kt", mappedClass("B"))
        fixture.build(task)
        registry() shouldContain "com.example.A"
        registry() shouldContain "com.example.B"
        shouldMatchCleanBuild(fixture)
    }

    case("INC-05", "ファイルを削除すると対応する出力が消え、集約出力が更新される", pending = PendingCase(Milestone.M2)) {
        val fixture =
            acg45Project {
                kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A"))
                kotlin("src/main/kotlin/com/example/B.kt", mappedClass("B"))
            }
        fixture.build(task)
        fixture.projectDir.resolve("src/main/kotlin/com/example/B.kt").delete() shouldBe true
        fixture.build(task)
        fixture.generatedNames(native) shouldNotContain "BMapper.kt"
        fixture
            .generatedFiles(native)
            .entries
            .single { it.key.endsWith("TpRegistry.kt") }
            .value shouldNotContain "com.example.B"
        shouldMatchCleanBuild(fixture)
    }

    case("INC-06", "ファイルの移動・リネームで出力が正しく付け替わる", pending = PendingCase(Milestone.M2)) {
        val fixture = acg45Project { kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A")) }
        fixture.build(task)
        // ファイル名を変え（A.kt → Renamed.kt）、パッケージも移す（com.example → com.moved）
        fixture.projectDir.resolve("src/main/kotlin/com/example/A.kt").delete()
        fixture.kotlin("src/main/kotlin/com/moved/Renamed.kt", mappedClass("A", pkg = "com.moved"))
        fixture.build(task)
        val generated = fixture.generatedFiles(native)
        generated.keys.filter { it.endsWith("AMapper.kt") }.single() shouldContain "com/moved/"
        generated.entries.single { it.key.endsWith("TpRegistry.kt") }.value shouldContain "com.moved.A"
        generated.entries.single { it.key.endsWith("TpRegistry.kt") }.value shouldNotContain "com.example.A"
        shouldMatchCleanBuild(fixture)
    }

    case("INC-07", "関数本体だけの変更（ABI 不変）でも、それを読む出力は再生成される", pending = PendingCase(Milestone.M2)) {
        val fixture =
            acg45Project(processors = "Body") {
                kotlin(
                    "src/main/kotlin/com/example/Calls.kt",
                    """
                    package com.example

                    import me.tbsten.analysiscodegen.tp.CallGraph

                    fun first() = 1
                    fun second() = 2

                    @CallGraph
                    fun entry(): Int = first()
                    """,
                )
            }
        fixture.build(task)
        val graph = {
            fixture
                .generatedFiles(native)
                .entries
                .single { it.key.endsWith("EntryCallGraph.kt") }
                .value
        }
        graph() shouldContain "first"

        // シグネチャ（ABI）は同じまま、本体の呼び出し先だけを変える
        fixture.kotlin(
            "src/main/kotlin/com/example/Calls.kt",
            """
            package com.example

            import me.tbsten.analysiscodegen.tp.CallGraph

            fun first() = 1
            fun second() = 2

            @CallGraph
            fun entry(): Int = second()
            """,
        )
        fixture.build(task).task(task)?.outcome shouldBe TaskOutcome.SUCCESS
        graph() shouldContain "second"
        graph() shouldNotContain "first"
    }

    case("INC-08", "依存ライブラリのバージョンが変わると全件処理になる", pending = PendingCase(Milestone.M2)) {
        val probe = probeFile()

        fun dep(version: String) = "dependencies { implementation(\"org.jetbrains.kotlinx:kotlinx-coroutines-core:$version\") }"
        val fixture =
            acg45Project(processors = "Mapper,Probe", probeOut = probe, extraBuild = dep("1.10.1")) {
                kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A"))
                kotlin("src/main/kotlin/com/example/B.kt", mappedClass("B"))
            }
        fixture.build(task)
        fixture.buildScript = fixture.buildScript.replace(dep("1.10.1"), dep("1.10.2"))
        fixture.build(task).task(task)?.outcome shouldBe TaskOutcome.SUCCESS
        lastRound1Files(probe) shouldContainExactlyInAnyOrder setOf("A.kt", "B.kt")
    }

    case("INC-09", "Processor の JAR・バージョン・オプションが変わると全件処理になる", pending = PendingCase(Milestone.M2)) {
        val probe = probeFile()
        val fixture =
            acg45Project(processors = "Mapper,Probe", probeOut = probe, dsl = "arg(\"inc09.flag\", \"1\")") {
                kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A"))
                kotlin("src/main/kotlin/com/example/B.kt", mappedClass("B"))
            }
        fixture.build(task)

        // オプションを変える
        fixture.buildScript = fixture.buildScript.replace("arg(\"inc09.flag\", \"1\")", "arg(\"inc09.flag\", \"2\")")
        fixture.build(task)
        lastRound1Files(probe) shouldContainExactlyInAnyOrder setOf("A.kt", "B.kt")

        // Processor の classpath（JAR の集合）を変える。Provider を持たない JAR でも、Processor が読めるクラスが変わるので全件処理
        fixture.buildScript += "\n\ndependencies { analysisCodeGen(\"org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.4.0\") }\n"
        fixture.build(task)
        lastRound1Files(probe) shouldContainExactlyInAnyOrder setOf("A.kt", "B.kt")
    }

    case("INC-10", "コンパイラオプションが変わると再処理される", pending = PendingCase(Milestone.M2)) {
        val probe = probeFile()
        val fixture =
            acg45Project(processors = "Mapper,Probe", probeOut = probe) {
                kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A"))
                kotlin("src/main/kotlin/com/example/B.kt", mappedClass("B"))
            }
        fixture.build(task)
        fixture.buildScript += "\n\nkotlin { compilerOptions { freeCompilerArgs.add(\"-Xcontext-parameters\") } }\n"
        fixture.build(task).task(task)?.outcome shouldBe TaskOutcome.SUCCESS
        lastRound1Files(probe) shouldContainExactlyInAnyOrder setOf("A.kt", "B.kt")
    }

    case("INC-11", "上流 SourceSet の生成物が変わると下流が再処理される", pending = PendingCase(Milestone.M2)) {
        // F-KMP 相当の最小構成（commonMain → jvmMain）。commonMain の生成物が変わると jvmMain のタスクが再実行される
        val fixture =
            acg45Project {
                buildScript =
                    """
                    plugins {
                        kotlin("multiplatform") version "{{kotlinVersion}}"
                        id("me.tbsten.analysiscodegen") version "${Acg45Repo.acgVersion}"
                    }

                    kotlin { jvm() }

                    analysisCodeGen {
                        arg("tp.processors", "Mapper,Registry")
                        sourceSets {
                            commonMain { processor("${Acg45Repo.coordinate("test-processors-native")}") }
                            jvmMain { processor("${Acg45Repo.coordinate("test-processors-native")}") }
                        }
                    }
                    """.trimIndent()
                projectDir.resolve("src/main").renameTo(projectDir.resolve("src/commonMain"))
                kotlin("src/commonMain/kotlin/com/example/A.kt", mappedClass("A"))
                kotlin("src/jvmMain/kotlin/com/example/J.kt", mappedClass("J"))
            }
        fixture.build(":analysisCodeGenJvmMain")
        fixture.kotlin("src/commonMain/kotlin/com/example/A.kt", mappedClass("A", props = "val id: Int, val extra: Long"))
        val result = fixture.build(":analysisCodeGenJvmMain")
        result.task(":analysisCodeGenCommonMain")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":analysisCodeGenJvmMain")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    case("INC-12", "Processor が宣言した外部入力（リソースファイル）が変わると再処理される", pending = PendingCase(Milestone.M2)) {
        // 外部入力（src/main/resources/tp-resource.properties）を Processor が宣言して読む。
        // TP-Resource にはまだ外部入力の宣言が無い。保留を外す前に TP-Resource に足す（ADR-027 の後継、テストの期待は変えない）
        val probe = probeFile()
        val fixture =
            acg45Project(processors = "Resource,Probe", probeOut = probe) {
                kotlin(
                    "src/main/kotlin/com/example/G.kt",
                    """
                    package com.example

                    import me.tbsten.analysiscodegen.tp.GenerateJava

                    @GenerateJava
                    class G
                    """,
                )
                file("src/main/resources/tp-resource.properties", "greeting=hello")
            }
        fixture.build(task)
        fixture.file("src/main/resources/tp-resource.properties", "greeting=bye")
        fixture.build(task).task(task)?.outcome shouldBe TaskOutcome.SUCCESS
        lastRound1Files(probe) shouldContain "G.kt"
    }

    case("INC-16", "マルチモジュールで :lib を変えると :app が必要な分だけ再処理される", pending = PendingCase(Milestone.M2)) {
        val probe = probeFile()
        val fixture =
            acg45Project(processors = "Mapper,Probe", probeOut = probe) {
                includes += listOf(":lib", ":app")
                val v = Acg45Repo.acgVersion
                val tp = Acg45Repo.coordinate("test-processors-native")
                buildScript =
                    """
                    plugins {
                        kotlin("jvm") version "{{kotlinVersion}}" apply false
                        id("me.tbsten.analysiscodegen") version "$v" apply false
                    }
                    """.trimIndent()
                file("lib/build.gradle.kts", "plugins { kotlin(\"jvm\") }")
                file(
                    "app/build.gradle.kts",
                    """
                    plugins {
                        kotlin("jvm")
                        id("me.tbsten.analysiscodegen")
                    }
                    dependencies { implementation(project(":lib")) }
                    analysisCodeGen {
                        arg("tp.processors", "Mapper,Probe")
                        arg("tp.probe.out", "${probe.absolutePath.replace("\\", "/")}")
                        sourceSets { main { processor("$tp") } }
                    }
                    """,
                )
                projectDir.resolve("src").deleteRecursively()
                file("app/src/main/kotlin/me/tbsten/analysiscodegen/tp/TpAnnotations.kt", TpAnnotationsSource.content)
                kotlin("lib/src/main/kotlin/com/lib/Base.kt", "package com.lib\n\nopen class Base(val id: Int)")
                kotlin(
                    "app/src/main/kotlin/com/app/UsesLib.kt",
                    """
                    package com.app

                    import me.tbsten.analysiscodegen.tp.Mapped

                    @Mapped
                    class UsesLib(val base: com.lib.Base)
                    """,
                )
                kotlin("app/src/main/kotlin/com/app/Independent.kt", mappedClass("Independent", pkg = "com.app"))
            }
        fixture.build(":app:analysisCodeGenMain")
        // :lib の ABI を変える
        fixture.kotlin("lib/src/main/kotlin/com/lib/Base.kt", "package com.lib\n\nopen class Base(val id: Long)")
        fixture.build(":app:analysisCodeGenMain").task(":app:analysisCodeGenMain")?.outcome shouldBe TaskOutcome.SUCCESS
        // :lib の型を参照する UsesLib は再処理、参照しない Independent は再処理しない
        lastRound1Files(probe) shouldBe setOf("UsesLib.kt")
    }

    case("INC-17", "変更がラウンド 2 以降の生成物に波及する場合も、増分で正しく再生成される", pending = PendingCase(Milestone.M2)) {
        fun chain(depth: Int) =
            """
            package com.example

            import me.tbsten.analysiscodegen.tp.Chain

            @Chain(depth = $depth)
            class Root
            """
        val fixture = acg45Project(processors = "Chain") { kotlin("src/main/kotlin/com/example/Root.kt", chain(3)) }
        fixture.build(task)
        fixture.generatedNames(native) shouldContainAll listOf("RootChain1.kt", "RootChain2.kt", "RootChain3.kt")

        fixture.kotlin("src/main/kotlin/com/example/Root.kt", chain(1))
        fixture.build(task)
        fixture.generatedNames(native) shouldContain "RootChain1.kt"
        fixture.generatedNames(native) shouldNotContain "RootChain2.kt"
        fixture.generatedNames(native) shouldNotContain "RootChain3.kt"

        fixture.kotlin("src/main/kotlin/com/example/Root.kt", chain(4))
        fixture.build(task)
        fixture.generatedNames(native) shouldContain "RootChain4.kt"
        fixture.generatedNames(native) shouldNotBe emptySet<String>()
        shouldMatchCleanBuild(fixture, "Chain")
    }
})

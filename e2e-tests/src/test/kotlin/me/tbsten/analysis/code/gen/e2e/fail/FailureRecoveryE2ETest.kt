package me.tbsten.analysis.code.gen.e2e.fail

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysis.code.gen.e2e.support.Acg45Engine
import me.tbsten.analysis.code.gen.e2e.support.Acg45Repo
import me.tbsten.analysis.code.gen.e2e.support.acg45Project
import me.tbsten.analysis.code.gen.e2e.support.codegenTask
import me.tbsten.analysis.code.gen.e2e.support.generatedFiles
import me.tbsten.analysis.code.gen.e2e.support.generatedNames
import me.tbsten.analysis.code.gen.e2e.support.mappedClass
import me.tbsten.analysis.code.gen.testfoundation.E2ESpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import me.tbsten.analysis.code.gen.testprocessors.BadRender
import org.gradle.testkit.runner.TaskOutcome
import java.util.concurrent.TimeUnit

// 5.12 失敗からの回復（FAIL）。出力のトランザクションは ADR-014（staging → Build Commit、journal）
private val native = Acg45Engine.Native
private val task = codegenTask(native)

private fun badTarget(name: String) =
    """
    package com.example

    import me.tbsten.analysis.code.gen.tp.BadTarget

    @BadTarget
    class $name
    """.trimIndent()

class FailureRecoveryE2ETest : E2ESpec({
    case("FAIL-01", "Processor の例外でビルドが失敗し、出力ディレクトリに途中までの生成物が残らない", pending = PendingCase(Milestone.M1)) {
        // TP-Bad の throw はファイルを 1 つ作ってから例外を投げる
        val fixture =
            acg45Project(processors = "Mapper,Bad", dsl = "arg(\"tp.bad.modes\", \"throw\")") {
                kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A"))
                kotlin("src/main/kotlin/com/example/T.kt", badTarget("T"))
            }
        val result = fixture.buildAndFail(task)
        result.task(task)?.outcome shouldBe TaskOutcome.FAILED
        result.output shouldContain BadRender.THROW_MESSAGE
        fixture.generatedFiles(native) shouldBe emptyMap()
    }

    case("FAIL-02", "失敗のあとソースを直して再ビルドすると、正しく成功する（増分キャッシュが汚れていない）", pending = PendingCase(Milestone.M2)) {
        val fixture =
            acg45Project(processors = "Mapper,Registry,Bad", dsl = "arg(\"tp.bad.modes\", \"throw\")") {
                kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A"))
            }
        fixture.build(task)
        // 1) 失敗させる
        fixture.kotlin("src/main/kotlin/com/example/T.kt", badTarget("T"))
        fixture.kotlin("src/main/kotlin/com/example/B.kt", mappedClass("B"))
        fixture.buildAndFail(task)
        // 2) 直す（@BadTarget を外す）
        fixture.projectDir.resolve("src/main/kotlin/com/example/T.kt").delete()
        fixture.build(task).task(task)?.outcome shouldBe TaskOutcome.SUCCESS
        fixture.generatedNames(native) shouldContain "BMapper.kt"

        // 3) 同じ最終入力のクリーンビルドと一致する
        val clean =
            acg45Project(processors = "Mapper,Registry,Bad", dsl = "arg(\"tp.bad.modes\", \"throw\")") {
                kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A"))
                kotlin("src/main/kotlin/com/example/B.kt", mappedClass("B"))
            }
        clean.build(task)
        fixture.generatedFiles(native) shouldBe clean.generatedFiles(native)
    }

    case("FAIL-03", "ビルドを強制終了（kill -9）したあとの次のビルドで、出力とキャッシュが整合している", pending = PendingCase(Milestone.M2)) {
        val fixture =
            acg45Project(processors = "Mapper,Registry") {
                kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A"))
                gradleProperties["org.gradle.daemon"] = "false"
            }
        fixture.build(task)
        repeat(200) { i -> fixture.kotlin("src/main/kotlin/com/example/Many$i.kt", mappedClass("Many$i")) }

        // 別プロセスの Gradle を起動し、生成の途中（staging に書き始めたころ）で kill -9 する
        val gradlew = java.io.File(Acg45Repo.root, "gradlew").absolutePath
        val process =
            ProcessBuilder(gradlew, "-p", fixture.projectDir.absolutePath, task.removePrefix(":"), "--no-daemon")
                .redirectErrorStream(true)
                .redirectOutput(fixture.projectDir.resolve("killed-build.log"))
                .start()
        val staging = fixture.projectDir.resolve("build/analysisCodeGen/staging")
        val deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(5)
        while (!staging.exists() && process.isAlive && System.nanoTime() < deadline) Thread.sleep(50)
        process.toHandle().descendants().forEach { it.destroyForcibly() }
        process.destroyForcibly().waitFor()

        // 次のビルドは journal を見て全件処理し（ADR-014）、クリーンビルドと同じ結果になる
        fixture.build(task).task(task)?.outcome shouldBe TaskOutcome.SUCCESS
        val clean = acg45Project(processors = "Mapper,Registry")
        fixture.projectDir.resolve("src").copyRecursively(clean.projectDir.resolve("src"), overwrite = true)
        clean.build(task)
        fixture.generatedFiles(native) shouldBe clean.generatedFiles(native)
    }

    case("FAIL-05", "生成物が原因でコンパイルエラーになったとき、生成物が残って原因を追える", pending = PendingCase(Milestone.M1)) {
        // TP-Mapper はプロパティの型をそのまま書く。生成物から参照できない private な型を使うと、生成後のコンパイルで失敗する
        val fixture =
            acg45Project(processors = "Mapper") {
                kotlin(
                    "src/main/kotlin/com/example/Hidden.kt",
                    """
                    package com.example

                    import me.tbsten.analysis.code.gen.tp.Mapped

                    private class Secret

                    @Mapped
                    class Hidden(private val secret: Secret, val id: Int)
                    """,
                )
            }
        val result = fixture.buildAndFail("compileKotlin")
        result.task(task)?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":compileKotlin")?.outcome shouldBe TaskOutcome.FAILED
        fixture.generatedNames(native) shouldContain "HiddenMapper.kt"
    }
})

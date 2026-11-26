package me.tbsten.analysis.code.gen.e2e.gcc

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import me.tbsten.analysis.code.gen.e2e.support.Acg45Engine
import me.tbsten.analysis.code.gen.e2e.support.acg45Project
import me.tbsten.analysis.code.gen.e2e.support.codegenTask
import me.tbsten.analysis.code.gen.e2e.support.generatedFiles
import me.tbsten.analysis.code.gen.e2e.support.mappedClass
import me.tbsten.analysis.code.gen.testfoundation.E2ESpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import org.gradle.testkit.runner.TaskOutcome
import java.nio.file.Files

// 5.13 Gradle のキャッシュ（GCC）。タスクは @CacheableTask の AnalysisCodegenTask 1 つ（ADR-016）
private val native = Acg45Engine.Native
private val task = codegenTask(native)

class GradleCacheE2ETest : E2ESpec({
    case(
        "GCC-01",
        "Configuration Cache を有効にして 2 回目で再利用され、問題の報告が 0 件（--configuration-cache-problems=fail）",
        pending = PendingCase(Milestone.M1),
    ) {
        val fixture = acg45Project { kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A")) }
        val args = arrayOf(task, "--configuration-cache", "--configuration-cache-problems=fail")
        val first = fixture.build(*args)
        first.output shouldContain "Configuration cache entry stored"
        first.task(task)?.outcome shouldBe TaskOutcome.SUCCESS

        // 入力を変えても設定は再利用され、タスクは実行される
        fixture.kotlin("src/main/kotlin/com/example/B.kt", mappedClass("B"))
        val second = fixture.build(*args)
        second.output shouldContain "Configuration cache entry reused"
        second.task(task)?.outcome shouldBe TaskOutcome.SUCCESS
    }

    case("GCC-02", "別ディレクトリのクリーンな checkout で Build Cache から復元される（FROM-CACHE、relocatable）", pending = PendingCase(Milestone.M2)) {
        // ローカルの Build Cache を 2 つの checkout で共有する（TestKit は独自の Gradle user home を使うため init script で指定）
        val cacheDir = Files.createTempDirectory("acg-build-cache").toFile()
        val init = Files.createTempFile("acg-build-cache", ".init.gradle.kts").toFile()
        init.writeText(
            "settingsEvaluated { buildCache { local { directory = file(\"${cacheDir.absolutePath.replace("\\", "/")}\") } } }\n",
        )

        fun checkout() =
            acg45Project {
                kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A"))
                kotlin("src/main/kotlin/com/example/B.kt", mappedClass("B"))
            }
        val first = checkout()
        first.build(task, "--build-cache", "-I", init.absolutePath).task(task)?.outcome shouldBe TaskOutcome.SUCCESS

        // 別の一時ディレクトリ（= 絶対パスが違う）で同じ内容をビルドすると、キャッシュから復元される
        val second = checkout()
        (second.projectDir.absolutePath == first.projectDir.absolutePath) shouldBe false
        second.build(task, "--build-cache", "-I", init.absolutePath).task(task)?.outcome shouldBe TaskOutcome.FROM_CACHE
        second.generatedFiles(native) shouldBe first.generatedFiles(native)
    }

    case("GCC-03", "入力が変わらないビルドで analysisCodeGen タスクが UP-TO-DATE になる（時間は PERF-09）", pending = PendingCase(Milestone.M2)) {
        val fixture =
            acg45Project {
                kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A"))
            }
        fixture.build("compileKotlin")
        // 生成物を使うコンパイルまで含めて、2 回目は何も実行しない
        val second = fixture.build("compileKotlin")
        second.task(task)?.outcome shouldBe TaskOutcome.UP_TO_DATE
        second.task(":compileKotlin")?.outcome shouldBe TaskOutcome.UP_TO_DATE
        // ビルドに関係しないファイル（README）を変えても UP-TO-DATE のまま
        fixture.file("README.md", "changed")
        fixture.build("compileKotlin").task(task)?.outcome shouldBe TaskOutcome.UP_TO_DATE
    }

    case("GCC-05", "Isolated Projects を有効にしても警告なしで動く", pending = PendingCase(Milestone.M3)) {
        val fixture = acg45Project { kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A")) }
        fixture.gradleProperties["org.gradle.unsafe.isolated-projects"] = "true"
        val result = fixture.build(task)
        result.task(task)?.outcome shouldBe TaskOutcome.SUCCESS
        result.output shouldNotContain "problems were found"
    }
})

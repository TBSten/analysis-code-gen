package me.tbsten.analysiscodegen.e2e.ver

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysiscodegen.e2e.support.SampleEngine
import me.tbsten.analysiscodegen.e2e.support.copyFixtureDir
import me.tbsten.analysiscodegen.e2e.support.generatedFiles
import me.tbsten.analysiscodegen.e2e.support.gradleArgs
import me.tbsten.analysiscodegen.e2e.support.repoRoot
import me.tbsten.analysiscodegen.testfoundation.DiffSpec
import me.tbsten.analysiscodegen.testfoundation.E2ESpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import me.tbsten.analysiscodegen.testfoundation.UnitSpec
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files

// 5.23 バージョン互換と診断（VER）。VER-02（analysis-code-gen-processor-api の版違い、I）は AA の型に触れるので :engine-tests に置く
// 対応 Kotlin の唯一の一覧は gradle/kotlin-versions.txt（ADR-011）。AGP の対応表は gradle/agp-versions.txt（ADR-037 で置き場を決めた）

/** 対応表の 1 行（`<版> <役割> [pr]`）。 */
private data class SupportedVersion(val version: String, val role: String, val pr: Boolean)

private fun readVersionList(file: File): List<SupportedVersion> =
    file
        .readLines()
        .map { it.substringBefore('#').trim() }
        .filter { it.isNotEmpty() }
        .map { line ->
            val parts = line.split(Regex("\\s+"))
            SupportedVersion(parts[0], parts[1], parts.getOrNull(2) == "pr")
        }

private val kotlinVersions get() = readVersionList(repoRoot.resolve("gradle/kotlin-versions.txt"))

/** F-JVM を Kotlin [kotlin] と [engine] でビルドする。-P は後に書いたものが勝つので、版の指定は最後に置く。 */
private fun buildFixtureWithKotlin(
    dir: File,
    kotlin: String,
    engine: SampleEngine = SampleEngine.Native,
    vararg extra: String,
): GradleRunner =
    GradleRunner
        .create()
        .withProjectDir(dir)
        .withArguments(listOf("clean", "compileKotlin") + gradleArgs(engine) + extra + "-Pacg.kotlin=$kotlin")
        .forwardOutput()

/** 「対応外」の診断に要るもの: 使われた版・原因・対応している版の一覧（ADR-011）。 */
private fun BuildResult.shouldExplainUnsupportedKotlin(used: String) {
    output shouldContain used
    output shouldContain "analysisCodeGen"
    // 対応している版をすべて並べる（利用者がどこへ上げ下げすればよいか分かるように）
    kotlinVersions.forEach { output shouldContain it.version }
}

/** `python3 scripts/kotlin-versions.py <args>` を [list] に対して流す。 */
private fun kotlinVersionsScript(
    list: File,
    vararg args: String,
): Int {
    val process =
        ProcessBuilder(listOf("python3", repoRoot.resolve("scripts/kotlin-versions.py").absolutePath, "--list", list.absolutePath) + args)
            .redirectErrorStream(true)
            .start()
    process.inputStream.bufferedReader().readText()
    return process.waitFor()
}

private fun copyOfKotlinVersions(): File =
    Files.createTempFile("kotlin-versions", ".txt").toFile().apply { writeText(repoRoot.resolve("gradle/kotlin-versions.txt").readText()) }

class VersionCompatibilityE2ETest : E2ESpec({
    case(
        "VER-01",
        "対応表に無い Kotlin / KGP の組み合わせでは、原因と対応バージョンが分かる診断が出る",
        pending = PendingCase(Milestone.M2),
    ) {
        val dir = copyFixtureDir("jvm")
        // 最低対応版（2.4.0）より古い Kotlin
        val old = buildFixtureWithKotlin(dir, "2.3.20").buildAndFail()
        old.shouldExplainUnsupportedKotlin("2.3.20")
        // 一覧に無い新しすぎる版（最新の次のメジャー）
        val tooNew = buildFixtureWithKotlin(dir, "9.0.0").buildAndFail()
        tooNew.shouldExplainUnsupportedKotlin("9.0.0")
    }

    case("VER-03", "対応表の最小と最新の AGP でビルドが通る", pending = PendingCase(Milestone.M2)) {
        // gradle/agp-versions.txt: `<版> min|latest`。F-AND を両方の版でビルドし、生成物を使うテストまで通す
        val agp = readVersionList(repoRoot.resolve("gradle/agp-versions.txt"))
        val versions = listOf("min", "latest").map { role -> agp.single { it.role == role }.version }
        for (version in versions) {
            val dir = copyFixtureDir("android")
            val result =
                GradleRunner
                    .create()
                    .withProjectDir(dir)
                    .withArguments(listOf(":app:testFreeDebugUnitTest") + gradleArgs(SampleEngine.Native) + "-Pacg.agp=$version")
                    .forwardOutput()
                    .build()
            withClue("AGP $version") { result.output shouldContain "BUILD SUCCESSFUL" }
            generatedFiles(dir.resolve("app"), SampleEngine.Native, "freeDebug").shouldNotBeEmpty()
        }
    }

    case(
        "VER-06",
        "プレリリースが stable になったら、プレリリース枠が次の最新に移り、古いプレリリースには対応外の診断が出る",
        pending = PendingCase(Milestone.M3),
    ) {
        val prerelease = kotlinVersions.single { it.role == "prerelease" }.version
        val stable = prerelease.substringBefore('-')
        // 一覧の更新: プレリリースの stable 版を足すと、プレリリースの行が消える（次のプレリリースは出たときに足す）
        val list = copyOfKotlinVersions()
        kotlinVersionsScript(list, "add", stable) shouldBe 0
        val updated = readVersionList(list)
        updated.map { it.version } shouldContain stable
        updated.map { it.version } shouldNotContain prerelease
        updated.single { it.version == stable }.pr shouldBe true
        // 古いプレリリース（一覧から外れた版）でビルドすると対応外の診断が出る。plugin は一覧を埋め込むので、
        // 一覧を書き換えた版の plugin を publishFixtureDeps し直してから流す
        val publish =
            GradleRunner
                .create()
                .withProjectDir(repoRoot)
                .withArguments("publishFixtureDeps", "-Pacg.kotlinVersionsFile=${list.absolutePath}")
                .forwardOutput()
                .build()
        publish.output shouldContain "BUILD SUCCESSFUL"
        val dir = copyFixtureDir("jvm")
        buildFixtureWithKotlin(dir, prerelease).buildAndFail().shouldExplainUnsupportedKotlin(prerelease)
    }
})

class VersionListUnitTest : UnitSpec({
    case(
        "VER-04",
        "Kotlin の新しい stable / プレリリースが出たら、対応バージョンの一覧と CI のマトリクスが自動で更新される（更新の PR が作られる）",
        pending = PendingCase(Milestone.M2),
    ) {
        val latestStable = kotlinVersions.filter { it.role == "stable" }.last().version
        val (major, minor, patch) = latestStable.split('.').map { it.toInt() }
        val nextStable = "$major.$minor.${patch + 10}"
        val nextPrerelease = "$major.${minor + 2}.0-Beta1"

        // 新しい stable: 一覧の末尾の stable になり、PR の matrix は baseline と新しい最新 stable の 2 つに移る
        val list = copyOfKotlinVersions()
        kotlinVersionsScript(list, "add", nextStable) shouldBe 0
        readVersionList(list).let { updated ->
            updated.filter { it.pr }.map { it.version } shouldBe listOf("2.4.0", nextStable)
            updated.single { it.version == latestStable }.pr shouldBe false
        }
        kotlinVersionsScript(list, "check") shouldBe 0

        // 新しいプレリリース: プレリリース枠は最新の 1 つだけ
        kotlinVersionsScript(list, "add", nextPrerelease) shouldBe 0
        readVersionList(list).filter { it.role == "prerelease" }.map { it.version } shouldBe listOf(nextPrerelease)
        kotlinVersionsScript(list, "check") shouldBe 0

        // 上流の新しい版を見つけるワークフローが、この script で一覧を直して PR を作る
        val workflows =
            repoRoot
                .resolve(".github/workflows")
                .listFiles()
                .orEmpty()
                .map { it.readText() }
        workflows.any { "scripts/kotlin-versions.py add" in it && ("gh pr create" in it || "create-pull-request" in it) } shouldBe true
    }
})

class VersionOutputDiffTest : DiffSpec({
    case(
        "VER-05",
        "同じ入力に対して、対応するすべての Kotlin バージョンで生成物が一致する（言語機能の差が出ない入力に限る）",
        pending = PendingCase(Milestone.M2),
    ) {
        // F-JVM の入力は 2.4.0 の言語機能だけで書いてある（ADR-027）。TP の Native 版で、版ごとの生成物を比べる
        val outputs =
            kotlinVersions.associate { v ->
                val dir = copyFixtureDir("jvm")
                buildFixtureWithKotlin(dir, v.version).build()
                v.version to generatedFiles(dir, SampleEngine.Native, "main")
            }
        val baseline = outputs.getValue("2.4.0")
        baseline.shouldNotBeEmpty()
        outputs.forEach { (version, files) -> withClue("Kotlin $version") { files shouldBe baseline } }
    }
})

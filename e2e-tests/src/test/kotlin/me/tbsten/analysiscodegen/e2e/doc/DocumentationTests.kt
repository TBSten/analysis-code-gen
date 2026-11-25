package me.tbsten.analysiscodegen.e2e.doc

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysiscodegen.e2e.support.SampleEngine
import me.tbsten.analysiscodegen.e2e.support.TestReport
import me.tbsten.analysiscodegen.e2e.support.copyProject
import me.tbsten.analysiscodegen.e2e.support.gradleArgs
import me.tbsten.analysiscodegen.e2e.support.repoRoot
import me.tbsten.analysiscodegen.testfoundation.E2ESpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import me.tbsten.analysiscodegen.testfoundation.UnitSpec
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

// 5.26 ドキュメント（DOC）。ページのコード例の出所は fixtures/docs/<ページ>（ADR-031）で、
// check_snippets.py が「ページの例 = 出所の一部」を、ここの E2E が「出所がそのままビルド・実行できる」を受け持つ

private fun exec(
    dir: File,
    vararg command: String,
    timeoutMinutes: Long = 20,
): Pair<Int, String> {
    val process = ProcessBuilder(*command).directory(dir).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText()
    check(process.waitFor(timeoutMinutes, TimeUnit.MINUTES)) { "${command.joinToString(" ")} が終わらない" }
    return process.exitValue() to output
}

/** fixtures/docs/<name> を一時ディレクトリに写して Gradle で流す（docs の例は analysisCodeGen の plugin を build/fixture-repo から読む）。 */
private fun buildDocsFixture(
    name: String,
    vararg tasks: String,
): Pair<File, BuildResult> {
    val dir = Files.createTempDirectory("acg-docs-$name").toFile()
    copyProject(repoRoot.resolve("fixtures/docs/$name"), dir)
    val result =
        GradleRunner
            .create()
            .withProjectDir(
                dir,
            ).withArguments(tasks.toList() + gradleArgs(SampleEngine.Native))
            .forwardOutput()
            .build()
    return dir to result
}

/** check_snippets.py が [pages] で不一致 0 件（ページの例が出所から離れていない）。 */
private fun snippetsShouldMatch(vararg pages: String) {
    val (code, output) = exec(repoRoot, "python3", "scripts/docsnippets/check_snippets.py", *pages)
    withClue(output) { code shouldBe 0 }
}

/** docs のページ（.md / .mdx）すべて。 */
private fun docsPages(): List<File> =
    repoRoot
        .resolve("docs/site/src/content/docs")
        .walkTopDown()
        .filter {
            it.isFile && (it.extension == "md" || it.extension == "mdx")
        }.toList()

class DocumentationUnitTest : UnitSpec({
    case("DOC-01", "ドキュメントサイトのビルドとリンクチェックが CI で通る", pending = PendingCase(Milestone.M1)) {
        // サイトのビルド（astro build）は starlight-links-validator でリンク切れを検査し、切れていたら失敗する
        val site = repoRoot.resolve("docs/site")
        val (install, installLog) = exec(site, "npm", "ci")
        withClue(installLog) { install shouldBe 0 }
        val (build, buildLog) = exec(site, "npm", "run", "build")
        withClue(buildLog) { build shouldBe 0 }
        site.resolve("dist/index.html").isFile shouldBe true
        // CI が PR ごとにサイトをビルドしている（ここで通っても CI で回っていなければ検出できない）
        repoRoot.resolve(".github/workflows").listFiles().orEmpty().any { wf ->
            val text = wf.readText()
            "docs/site" in text && "npm run build" in text && "pull_request" in text
        } shouldBe true
    }

    case("DOC-04", "Dokka の API reference が生成され、リンク切れが無い", pending = PendingCase(Milestone.M2)) {
        // 公開モジュールの API reference をまとめて生成する（acg.dokka-aggregate、ADR-031 で docs サイトの /api/ に置く）
        val result =
            GradleRunner
                .create()
                .withProjectDir(repoRoot)
                .withArguments(":dokkaGenerate", "--stacktrace")
                .forwardOutput()
                .build()
        result.output shouldContain "BUILD SUCCESSFUL"
        val html = repoRoot.resolve("build/dokka/html")
        val pages = html.walkTopDown().filter { it.isFile && it.extension == "html" }.toList()
        pages.shouldNotBeEmpty()
        // 公開 API の入口が載っている
        pages.any { "AnalysisCodegenProcessorProvider" in it.readText() } shouldBe true
        // ページ内の相対リンクがすべて実在するファイルを指す
        val href = Regex("""href="([^"#?]+)[^"]*"""")
        val broken =
            pages.flatMap { page ->
                href
                    .findAll(page.readText())
                    .map { it.groupValues[1] }
                    .filterNot { it.startsWith("http:") || it.startsWith("https:") || it.startsWith("mailto:") || it.startsWith("data:") }
                    .filterNot {
                        page.parentFile
                            .resolve(it)
                            .normalize()
                            .exists()
                    }.map { "${page.relativeTo(html)} -> $it" }
            }
        withClue(broken.take(20).joinToString("\n")) { broken.shouldBeEmpty() }
    }
})

class DocumentationE2ETest : E2ESpec({
    case("DOC-02", "Quick start と How to install のコード例が、そのまま E2E テストとしてビルド・実行される", pending = PendingCase(Milestone.M1)) {
        snippetsShouldMatch("docs/site/src/content/docs/start/quick-start.mdx", "docs/site/src/content/docs/start/install.mdx")

        // Quick start: `:app:run` で挨拶が出て、生成物が expected と同じ（fixtures/docs/quick-start/README.md）
        val (quick, appRun) = buildDocsFixture("quick-start", ":app:run")
        appRun.output shouldContain "Hello, Greeter!"
        val expected = repoRoot.resolve("fixtures/docs/quick-start/expected")
        expected.walkTopDown().filter { it.isFile }.forEach { file ->
            val rel = file.relativeTo(expected).invariantSeparatorsPath
            quick.resolve("app/build/generated/analysisCodeGen/main/kotlin/$rel").readText() shouldBe file.readText()
        }

        // How to install: jvm-app・kmp-lib・bridge-app がそのままビルドできる
        val (_, install) = buildDocsFixture("install", "build")
        listOf(":jvm-app:build", ":kmp-lib:build", ":bridge-app:build").forEach { task ->
            install.task(task)?.outcome?.name shouldBe "SUCCESS"
        }
    }

    case("DOC-03", "全ページのコード例が E2E テストとしてビルド・実行される", pending = PendingCase(Milestone.M2)) {
        // コード（kotlin / kts / java）の例を持つページは、すべて fixture を持ち、例に title（出所のパス）が付いている
        val code = Regex("""^\s*(`{3,}|~{3,})(kotlin|kt|kts|java)\b(.*)$""", RegexOption.MULTILINE)
        val unsourced =
            docsPages().flatMap { page ->
                val text = page.readText()
                val hasFixture = Regex("""(?m)^fixture:\s*\S+""").containsMatchIn(text.substringBefore("\n---", ""))
                code
                    .findAll(text)
                    .filter { m -> !hasFixture || "title=" !in m.groupValues[3] }
                    .map { "${page.relativeTo(repoRoot)}: ${it.value.trim()}" }
            }
        withClue(unsourced.joinToString("\n")) { unsourced.shouldBeEmpty() }
        snippetsShouldMatch()

        // 出所のフィクスチャをすべてビルドする。テストを持つものは 1 件以上走って全部通る
        val fixtures =
            repoRoot
                .resolve("fixtures/docs")
                .listFiles()
                .orEmpty()
                .filter { it.resolve("settings.gradle.kts").isFile }
        fixtures.size shouldBeGreaterThan 2
        for (fixture in fixtures) {
            val (dir, result) = buildDocsFixture(fixture.name, "build")
            withClue(fixture.name) {
                result.output shouldContain "BUILD SUCCESSFUL"
                val report = TestReport.read(dir)
                if (report.tests > 0) report.passedSomething shouldBe true
            }
        }
    }

    case("DOC-05", "移行ガイドの手順どおりに選定 Processor を移行すると、RW の E2E が通る", pending = PendingCase(Milestone.M3)) {
        // 移行ガイドの例の出所: fixtures/docs/migrate-ksp/<processor>/{before,after}。before は KSP2、after はガイドの手順を当てたもの（Bridge）。
        // after で、RW の B1 と同じく生成コードを使う実行時テストが before と同じ件数だけ通る
        snippetsShouldMatch()
        val root = repoRoot.resolve("fixtures/docs/migrate-ksp")
        for (processor in listOf("cream", "moshi", "koin", "kotlin-inject", "dagger")) {
            val reports =
                listOf("before", "after").associateWith { side ->
                    val (dir, result) = buildDocsFixture("migrate-ksp/$processor/$side", "test")
                    withClue("$processor/$side") { result.output shouldContain "BUILD SUCCESSFUL" }
                    TestReport.read(dir)
                }
            withClue(processor) {
                reports.getValue("after").passedSomething shouldBe true
                reports.getValue("after").tests shouldBe reports.getValue("before").tests
            }
            // after は KSP の Gradle plugin を使っていない（移行し切っている）
            root
                .resolve("$processor/after")
                .walkTopDown()
                .filter { it.isFile && it.extension == "kts" }
                .none { "com.google.devtools.ksp" in it.readText() } shouldBe true
        }
    }
})

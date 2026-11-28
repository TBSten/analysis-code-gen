package me.tbsten.analysis.code.gen.e2e.support

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files

// ACG-46 の補助（保留テスト③: RW・TST・VER・DOC・PERF・MEM）。ADR-037
// 実用 Processor のサンプル（fixtures/real-world）と、上流のテストスイートの実行をまとめる。

/** リポジトリのルート。テストの作業ディレクトリ（モジュール直下）から上にたどって探す。 */
internal val repoRoot: File by lazy {
    generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .firstOrNull { it.resolve("gradle/kotlin-versions.txt").isFile && it.resolve("quality").isDirectory }
        ?: error("リポジトリのルートが見つからない（user.dir=${System.getProperty("user.dir")}）")
}

/** サンプルを動かす実行系。`-Pacg.fixture.engine` の値（ADR-027・037）。 */
internal enum class SampleEngine(val property: String) {
    /** 公式 KSP2 + 元の Processor（比較の基準 K）。 */
    Ksp2("ksp"),

    /** analysisCodeGen の Bridge + 元の Processor（B）。 */
    Bridge("bridge"),

    /** analysisCodeGen + Native 書き直し版（N）。 */
    Native("native"),
}

/** `fixtures/processors/processors.toml`（ADR-019）の 1 件。版は直書きせずここから読む。 */
internal data class TargetProcessor(
    val id: String,
    val name: String,
    val version: String,
    val artifacts: List<String>,
    /** `github:<owner>/<repo>@<SHA>` か `maven-sources:...`。 */
    val source: String,
)

internal object TargetProcessors {
    val all: List<TargetProcessor> by lazy { parse(repoRoot.resolve("fixtures/processors/processors.toml").readText()) }

    fun of(id: String): TargetProcessor = all.firstOrNull { it.id == id } ?: error("processors.toml に $id が無い")

    // 標準ライブラリだけで読む。processors.toml は `key = "value"` と `key = ["a", "b"]` しか使わない（processors.py が検査）
    internal fun parse(toml: String): List<TargetProcessor> =
        toml.split("[[processor]]").drop(1).map { block ->
            val values =
                block
                    .lines()
                    .map { it.substringBefore(" #").trim() }
                    .filter { "=" in it && !it.startsWith("#") }
                    .associate { it.substringBefore("=").trim() to it.substringAfter("=").trim() }

            fun str(key: String) = values[key]?.removeSurrounding("\"") ?: error("processors.toml の $key が無い: $block")
            TargetProcessor(
                id = str("id"),
                name = str("name"),
                version = str("version"),
                artifacts = Regex("\"([^\"]+)\"").findAll(values["artifacts"].orEmpty()).map { it.groupValues[1] }.toList(),
                source = str("source"),
            )
        }
}

/**
 * 実用 Processor のサンプルプロジェクト（`fixtures/real-world/<processor>/<sample>`、ADR-037）。
 *
 * 各サンプルは独立した Gradle ビルドで、F-* と同じく `-Pacg.fixture.engine` で K / B / N を切り替える。
 * 生成コードを使う実行時テスト（`src/test`）を持ち、どの実行系でも同じテストが通ることを確かめる。
 */
internal class RealWorldSample private constructor(
    val processorId: String,
    val name: String,
    val projectDir: File,
) {
    /** サンプルのビルドを流す。成功を期待する。 */
    fun build(
        engine: SampleEngine,
        vararg tasks: String,
    ): BuildResult = runner(engine, *tasks).build()

    fun buildAndFail(
        engine: SampleEngine,
        vararg tasks: String,
    ): BuildResult = runner(engine, *tasks).buildAndFail()

    fun runner(
        engine: SampleEngine,
        vararg tasks: String,
    ): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withArguments(tasks.toList() + gradleArgs(engine))
            .forwardOutput()

    /** [module] の [sourceSet] で生成されたファイル（ルートからの相対パス → 中身、改行は LF）。 */
    fun generated(
        engine: SampleEngine,
        sourceSet: String = "main",
        module: String = "",
    ): Map<String, String> = generatedFiles(projectDir.resolve(module), engine, sourceSet)

    /** `build/test-results` の JUnit XML を集計する。 */
    fun testReport(module: String = ""): TestReport = TestReport.read(projectDir.resolve(module).resolve("build/test-results"))

    companion object {
        /** `fixtures/real-world/<processor>/<sample>` を一時ディレクトリに写して開く。 */
        fun open(
            processorId: String,
            name: String,
        ): RealWorldSample {
            val source = repoRoot.resolve("fixtures/real-world/${processorId.lowercase()}/$name")
            check(source.isDirectory) { "サンプル ${source.relativeTo(repoRoot)} がまだ無い（ADR-037 の置き場に作る）" }
            val dir = Files.createTempDirectory("acg-rw-${processorId.lowercase()}-$name").toFile()
            copyProject(source, dir)
            return RealWorldSample(processorId, name, dir)
        }
    }
}

/** ビルドに共通で渡す引数。Processor の版は processors.toml から渡す（ADR-019）。 */
internal fun gradleArgs(engine: SampleEngine): List<String> =
    buildList {
        add("-Pacg.fixture.engine=${engine.property}")
        // サンプルは一時ディレクトリに写して動かすので、build-support がリポジトリのルートを探せない
        add("-Pacg.repoRoot=${repoRoot.absolutePath}")
        add("-Pacg.fixture.repo=${repoRoot.resolve("build/fixture-repo").absolutePath}")
        System.getProperty("acg.kotlin")?.takeIf { it.isNotBlank() }?.let { add("-Pacg.kotlin=$it") }
        System.getProperty("acg.mavenMirror")?.takeIf { it.isNotBlank() }?.let { add("-Pacg.mavenMirror=$it") }
        TargetProcessors.all.forEach { add("-Pacg.processor.${it.id}.version=${it.version}") }
        add("--stacktrace")
    }

/** 生成物の置き場。K は KSP2 の、B / N は ADR-016 の SourceSet 単位の出力ディレクトリ。 */
internal fun generatedRoot(
    projectDir: File,
    engine: SampleEngine,
    sourceSet: String,
): File =
    when (engine) {
        SampleEngine.Ksp2 -> projectDir.resolve("build/generated/ksp/$sourceSet")
        SampleEngine.Bridge, SampleEngine.Native -> projectDir.resolve("build/generated/analysisCodeGen/$sourceSet")
    }

/** 生成物を「`kotlin/…`・`java/…`・`resources/…` の相対パス → 中身」で読む。差分テストでそのまま比べられる形（辞書順・LF）。 */
internal fun generatedFiles(
    projectDir: File,
    engine: SampleEngine,
    sourceSet: String,
): Map<String, String> {
    val root = generatedRoot(projectDir, engine, sourceSet)
    if (!root.isDirectory) return emptyMap()
    return root
        .walkTopDown()
        .filter { it.isFile }
        // KSP2 は classes/ にもコピーを置く。比べるのはソースとリソースだけ
        .filter { f -> listOf("kotlin", "java", "resources").any { f.relativeTo(root).path.startsWith(it) } }
        .associate { it.relativeTo(root).invariantSeparatorsPath to it.readText().replace("\r\n", "\n") }
        .toSortedMap()
}

/** JUnit XML の集計。実行時テストが「通った」と言えるのは、1 件以上走って失敗もエラーも無いとき。 */
internal data class TestReport(val tests: Int, val failures: Int, val errors: Int, val skipped: Int) {
    val passedSomething: Boolean get() = tests > 0 && failures == 0 && errors == 0

    companion object {
        private val SUITE = Regex("""<testsuite\b[^>]*>""")

        private fun attr(
            tag: String,
            name: String,
        ) = Regex("""\b$name="(\d+)"""")
            .find(tag)
            ?.groupValues
            ?.get(1)
            ?.toInt() ?: 0

        fun read(dir: File): TestReport {
            val tags =
                dir
                    .walkTopDown()
                    .filter { it.isFile && it.name.startsWith("TEST-") && it.extension == "xml" }
                    .flatMap { SUITE.findAll(it.readText()).map { m -> m.value } }
                    .toList()
            return TestReport(
                tests = tags.sumOf { attr(it, "tests") },
                failures = tags.sumOf { attr(it, "failures") },
                errors = tags.sumOf { attr(it, "errors") },
                skipped = tags.sumOf { attr(it, "skipped") },
            )
        }
    }
}

/**
 * 上流 Processor 自身のテストスイート（RW-*-B3）。processors.toml の固定コミットを取得し、
 * 初期化スクリプト `fixtures/real-world/init/ksp-to-bridge.init.gradle.kts` で KSP を Bridge に差し替えて流す（ADR-037）。
 */
internal object UpstreamSuite {
    /** 上流を `build/acg/upstream/<id>` に取得する（取得済みなら再利用）。 */
    fun checkout(id: String): File {
        val processor = TargetProcessors.of(id)
        val match = Regex("""github:([^@]+)@([0-9a-f]{40})""").matchEntire(processor.source)
        checkNotNull(match) { "$id の上流は GitHub の固定コミットではない: ${processor.source}" }
        val (repo, sha) = match.destructured
        val dir = repoRoot.resolve("build/acg/upstream/${id.lowercase()}")
        if (!dir.resolve(".git").isDirectory) {
            dir.mkdirs()
            git(dir, "init", "-q")
            git(dir, "remote", "add", "origin", "https://github.com/$repo.git")
        }
        git(dir, "fetch", "-q", "--depth", "1", "origin", sha)
        git(dir, "checkout", "-q", "--force", sha)
        return dir
    }

    /** 上流の [tasks] を [engine] で流す。Bridge なら初期化スクリプトで KSP を差し替える。 */
    fun run(
        dir: File,
        engine: SampleEngine,
        vararg tasks: String,
    ): BuildResult {
        val init = repoRoot.resolve("fixtures/real-world/init/ksp-to-bridge.init.gradle.kts")
        if (engine != SampleEngine.Ksp2) check(init.isFile) { "初期化スクリプト ${init.relativeTo(repoRoot)} がまだ無い（ADR-037）" }
        val args =
            buildList {
                addAll(tasks)
                add("--continue")
                if (engine != SampleEngine.Ksp2) addAll(listOf("--init-script", init.absolutePath))
                addAll(gradleArgs(engine))
            }
        return GradleRunner
            .create()
            .withProjectDir(dir)
            .withArguments(args)
            .forwardOutput()
            .build()
    }

    private fun git(
        dir: File,
        vararg args: String,
    ) {
        val process = ProcessBuilder(listOf("git") + args).directory(dir).redirectErrorStream(true).start()
        val out = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "git ${args.joinToString(" ")} が失敗: $out" }
    }
}

/**
 * プロジェクトを写す。settings の `includeBuild("相対パス")` は写し元からの絶対パスに直す
 * （F-* の build-support などを一時ディレクトリからも読めるように）。
 */
internal fun copyProject(
    source: File,
    target: File,
) {
    source.copyRecursively(target, overwrite = true)
    target
        .walkTopDown()
        .filter { it.isFile && it.name.startsWith("settings") && it.name.endsWith(".gradle.kts") }
        .forEach { file ->
            val origin = source.resolve(file.relativeTo(target).path).parentFile
            val text =
                Regex("""includeBuild\("([^"]+)"\)""").replace(file.readText()) { m ->
                    val path = m.groupValues[1]
                    if (File(
                            path,
                        ).isAbsolute
                    ) {
                        m.value
                    } else {
                        "includeBuild(\"${origin.resolve(path).canonicalFile.invariantSeparatorsPath}\")"
                    }
                }
            file.writeText(text)
        }
}

/**
 * コンパイル結果の公開 ABI（三者差分の段階 1、XD-05）。[classesDir] 以下の全 .class を `javap -public` で読み、
 * クラスごとの公開シグネチャを辞書順に並べる。ユーザーのコードは同じなので、差は生成物から来る。
 */
internal fun publicAbi(classesDir: File): Map<String, List<String>> {
    val javap = File(System.getProperty("java.home"), "bin/javap").takeIf { it.isFile }?.path ?: "javap"
    return classesDir
        .walkTopDown()
        .filter { it.isFile && it.extension == "class" }
        .sortedBy { it.relativeTo(classesDir).invariantSeparatorsPath }
        .associate { file ->
            val process = ProcessBuilder(javap, "-public", file.absolutePath).redirectErrorStream(true).start()
            val lines = process.inputStream.bufferedReader().readLines()
            check(process.waitFor() == 0) { "javap が失敗: $file\n${lines.joinToString("\n")}" }
            // 1 行目の "Compiled from ..." はソースファイル名なので比べない
            file.relativeTo(classesDir).invariantSeparatorsPath to
                lines.filterNot { it.startsWith("Compiled from") }.map { it.trim() }.sorted()
        }
}

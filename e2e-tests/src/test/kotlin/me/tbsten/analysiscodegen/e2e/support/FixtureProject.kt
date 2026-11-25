package me.tbsten.analysiscodegen.e2e.support

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files

/**
 * 共通フィクスチャ F-*（`fixtures/projects/<name>`）を一時ディレクトリへ複製して、TestKit でビルドする（ADR-027・032）。
 *
 * ```
 * val p = fixtureProject(Fixture.JVM)
 * p.build("build")
 * p.generated("main").kotlin("fixture/jvm/UserMapper.kt").isFile shouldBe true
 * ```
 *
 * - フィクスチャは `../build-support` を includeBuild するので、build-support も隣に複製する
 * - テスト用 Processor と plugin は `<repo>/build/fixture-repo` から読む。先に `./gradlew publishFixtureDeps` が要る
 * - 元のフィクスチャは書き換えない。[write] / [edit] は複製先だけを変える
 */
class FixtureProject internal constructor(
    /** 複製先のルート（`settings.gradle.kts` がある場所）。 */
    val dir: File,
) {
    /** `acg`（公開 DSL、既定）か `ksp`（KSP2 で同じソースをビルド）。 */
    var engine: String = "acg"

    /** 毎回のビルドに足す Gradle 引数。 */
    val extraArguments: MutableList<String> = mutableListOf()

    /** 使う Gradle の版。null なら TestKit を動かしている Gradle と同じ。 */
    var gradleVersion: String? = null

    /** 複製先の中のファイル。 */
    fun file(path: String): File = dir.resolve(path)

    /** 複製先にファイルを書く（親ディレクトリは作る）。 */
    fun write(
        path: String,
        content: String,
    ): File =
        file(path).apply {
            parentFile.mkdirs()
            writeText(content.trimIndent() + "\n")
        }

    /** 複製先のファイルを書き換える。 */
    fun edit(
        path: String,
        transform: (String) -> String,
    ): File =
        file(path).apply {
            check(isFile) { "$path が無い" }
            writeText(transform(readText()))
        }

    /** 複製先のファイルを消す。 */
    fun delete(path: String) {
        check(file(path).delete()) { "$path を消せない" }
    }

    /** モジュール（`""` はルート、`"app"` など）の生成物ディレクトリ。 */
    fun generated(
        sourceSet: String,
        module: String = "",
    ): GeneratedDir = GeneratedDir(moduleDir(module).resolve("build/generated/analysisCodeGen/$sourceSet"))

    /** Compilation 単位の実行（ADR-016）の生成物ディレクトリ。[unitName] は `jvmMain` など。 */
    fun generatedForCompilation(
        unitName: String,
        module: String = "",
    ): GeneratedDir = GeneratedDir(moduleDir(module).resolve("build/generated/analysisCodeGen/compilations/$unitName"))

    /** モジュールの `build/generated/analysisCodeGen` 以下の全ファイル（相対パス → 中身、辞書順）。 */
    fun allGenerated(module: String = ""): Map<String, String> =
        moduleDir(module).resolve("build/generated/analysisCodeGen").contentsByPath()

    fun moduleDir(module: String): File = if (module.isEmpty()) dir else dir.resolve(module.removePrefix(":").replace(':', '/'))

    /** 成功するはずのビルド。 */
    fun build(vararg arguments: String): BuildResult = runner(*arguments).build()

    /** 失敗するはずのビルド。 */
    fun buildAndFail(vararg arguments: String): BuildResult = runner(*arguments).buildAndFail()

    /** 細かく設定できる [GradleRunner]。 */
    fun runner(vararg arguments: String): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(dir)
            .withArguments(arguments.toList() + baseArguments())
            .forwardOutput()
            .apply { gradleVersion?.let { withGradleVersion(it) } }

    private fun baseArguments(): List<String> =
        buildList {
            add("--stacktrace")
            add("-Pacg.repoRoot=${E2EEnvironment.repoRoot.absolutePath}")
            add("-Pacg.fixture.engine=$engine")
            E2EEnvironment.kotlinVersion?.let { add("-Pacg.kotlin=$it") }
            addAll(extraArguments)
        }
}

/** 生成物ディレクトリ（`build/generated/analysisCodeGen/<SourceSet>`）。中に `kotlin` / `java` / `resources`（GRD-02）。 */
class GeneratedDir(val root: File) {
    fun kotlin(path: String = ""): File = root.resolve("kotlin").resolve(path)

    fun java(path: String = ""): File = root.resolve("java").resolve(path)

    fun resources(path: String = ""): File = root.resolve("resources").resolve(path)

    /** 全ファイル（`kotlin/...` などの相対パス → 中身、辞書順）。 */
    fun files(): Map<String, String> = root.contentsByPath()

    /** `.kt` / `.java` のファイル名（拡張子なし）の一覧。 */
    fun sourceNames(): List<String> =
        files()
            .keys
            .filter {
                it.endsWith(".kt") || it.endsWith(".java")
            }.map { it.substringAfterLast('/').substringBeforeLast('.') }
}

/** F-* の名前と置き場（`fixtures/README.md`）。 */
enum class Fixture(val dirName: String) {
    JVM("jvm"),
    JVM_MULTI("jvm-multi"),
    JAVA("java"),
    KMP("kmp"),
    KMP_CUSTOM("kmp-custom"),
    ANDROID("android"),
}

/** F-* を一時ディレクトリへ複製する。[block] で複製先を書き換えられる（ビルドはしない）。 */
fun fixtureProject(
    fixture: Fixture,
    block: FixtureProject.() -> Unit = {},
): FixtureProject = copyFixture(fixture.dirName).apply(block)

/**
 * F-* を使わない小さなプロジェクトを作る。settings は F-* と同じ（`acg.fixture-settings`）なので、
 * `libs` / `acg` の version catalog と `me.tbsten.analysis.code.gen` plugin がそのまま使える。
 *
 * ```
 * val p = emptyProject {
 *     write("build.gradle.kts", """plugins { alias(libs.plugins.kotlin.jvm); id("me.tbsten.analysis.code.gen") }""")
 * }
 * ```
 */
fun emptyProject(
    includes: List<String> = emptyList(),
    block: FixtureProject.() -> Unit = {},
): FixtureProject {
    val root = Files.createTempDirectory("acg-e2e").toFile()
    copyBuildSupport(root)
    val dir = root.resolve("project").apply { mkdirs() }
    return FixtureProject(dir).apply {
        write(
            "settings.gradle.kts",
            """
            pluginManagement {
                includeBuild("../build-support")
            }

            plugins {
                id("acg.fixture-settings")
            }

            rootProject.name = "acg-e2e"
            ${includes.joinToString("\n") { "include(\"$it\")" }}
            """,
        )
        write("gradle.properties", "org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8\nkotlin.code.style=official")
        write("src/main/kotlin/me/tbsten/analysiscodegen/tp/TpAnnotations.kt", tpAnnotationsSource())
        block()
    }
}

/** テスト用 Processor のアノテーション（F-JVM に置いてあるものと同じ）。 */
fun tpAnnotationsSource(): String =
    E2EEnvironment.repoRoot.resolve("fixtures/projects/jvm/src/main/kotlin/me/tbsten/analysiscodegen/tp/TpAnnotations.kt").readText()

private fun copyFixture(name: String): FixtureProject {
    val source = E2EEnvironment.repoRoot.resolve("fixtures/projects/$name")
    check(source.resolve("settings.gradle.kts").isFile) { "フィクスチャ $name が無い: $source" }
    val root = Files.createTempDirectory("acg-e2e").toFile()
    copyBuildSupport(root)
    val dir = root.resolve(name)
    source.copyRecursively(dir) { _, e -> throw e }
    dir
        .walkTopDown()
        .filter { it.isDirectory && (it.name == "build" || it.name == ".gradle") }
        .toList()
        .forEach { it.deleteRecursively() }
    return FixtureProject(dir)
}

private fun copyBuildSupport(root: File) {
    val buildSupport = E2EEnvironment.repoRoot.resolve("fixtures/projects/build-support")
    buildSupport.copyRecursively(root.resolve("build-support")) { _, e -> throw e }
    root
        .resolve("build-support")
        .walkTopDown()
        .filter {
            it.isDirectory && (it.name == "build" || it.name == ".gradle")
        }.toList()
        .forEach { it.deleteRecursively() }
}

internal fun File.contentsByPath(): Map<String, String> {
    if (!isDirectory) return emptyMap()
    return walkTopDown()
        .filter { it.isFile }
        .associate { it.relativeTo(this).invariantSeparatorsPath to it.readText() }
        .toSortedMap()
}

/** e2e-tests の build.gradle.kts から渡す値。 */
object E2EEnvironment {
    /** リポジトリのルート。system property `acg.repoRoot`（無ければ作業ディレクトリから上へたどる）。 */
    val repoRoot: File by lazy {
        System.getProperty("acg.repoRoot")?.let(::File)
            ?: generateSequence(File("").absoluteFile) { it.parentFile }.first { it.resolve("fixtures/projects/build-support").isDirectory }
    }

    /** フィクスチャの Kotlin（ADR-011）。null なら catalog の版。 */
    val kotlinVersion: String? get() = System.getProperty("acg.kotlin")?.takeIf { it.isNotBlank() }
}

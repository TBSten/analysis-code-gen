package me.tbsten.analysis.code.gen.testing.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.io.File
import java.nio.file.Files

/**
 * Gradle TestKit でビルドするフィクスチャプロジェクト（TST-06、ADR-021）。
 *
 * ```
 * val fixture = gradleFixture {
 *     kotlinJvm()
 *     kotlin("src/main/kotlin/Foo.kt", "class Foo")
 * }
 * fixture.build("compileKotlin")
 * ```
 *
 * - Kotlin の版は [kotlinVersion]。既定は system property `acg.kotlin`（CI の matrix、ADR-011）、無ければこの testing が使っているコンパイラの版
 * - リポジトリは Maven Central と Google。system property `acg.mavenMirror` があれば先に見る（429 を返す環境向け）
 * - TestKit は独自の Gradle user home を使うので、`~/.gradle/init.d` は効かない。ミラーはこのクラスが settings に書く
 */
public class GradleFixture internal constructor(public val projectDir: File) {
    /** 使う Kotlin の版。 */
    public var kotlinVersion: String = defaultKotlinVersion()

    /** settings.gradle.kts の `rootProject.name`。 */
    public var rootProjectName: String = "fixture"

    /** プラグイン解決に足すリポジトリ（`pluginManagement.repositories` の中身。1 行 1 つ）。 */
    public val extraPluginRepositories: MutableList<String> = mutableListOf()

    /** 依存解決に足すリポジトリ（`dependencyResolutionManagement.repositories` の中身。1 行 1 つ）。 */
    public val extraRepositories: MutableList<String> = mutableListOf()

    /** `include(...)` するサブプロジェクト（例: `:lib`）。 */
    public val includes: MutableList<String> = mutableListOf()

    /** ルートの build.gradle.kts。[kotlinJvm] などで組み立てるか、直接書く。`{{kotlinVersion}}` は [kotlinVersion] に置き換わる。 */
    public var buildScript: String = ""

    /** gradle.properties に書く値。 */
    public val gradleProperties: MutableMap<String, String> = linkedMapOf("org.gradle.jvmargs" to "-Xmx1g")

    /** TestKit に渡す、テスト対象の Gradle プラグインの classpath（`GradleRunner.withPluginClasspath`）。 */
    public val pluginClasspath: MutableList<File> = mutableListOf()

    /** 使う Gradle の版。null なら TestKit を動かしている Gradle と同じ。 */
    public var gradleVersion: String? = null

    /** kotlin("jvm") 1 モジュールの build.gradle.kts にする。 */
    public fun kotlinJvm(extra: String = "") {
        buildScript =
            """
            plugins {
                kotlin("jvm") version "{{kotlinVersion}}"
            }

            kotlin {
                jvmToolchain(${Runtime.version().feature()})
            }
            """.trimIndent() + if (extra.isBlank()) "" else "\n\n" + extra.trimIndent()
    }

    /** プロジェクト内にファイルを書く（親ディレクトリは作る）。 */
    public fun file(
        path: String,
        content: String,
    ): File =
        projectDir.resolve(path).apply {
            parentFile.mkdirs()
            writeText(content.trimIndent().replace("{{kotlinVersion}}", kotlinVersion))
        }

    /** `src/main/kotlin` 以下などに Kotlin ファイルを書く。 */
    public fun kotlin(
        path: String,
        content: String,
    ): File = file(path, content)

    /** 設定ファイルを書き出して、成功するはずのビルドを流す。 */
    public fun build(vararg arguments: String): BuildResult = runner(*arguments).build()

    /** 設定ファイルを書き出して、失敗するはずのビルドを流す。 */
    public fun buildAndFail(vararg arguments: String): BuildResult = runner(*arguments).buildAndFail()

    /** 設定ファイルを書き出して、細かく設定できる [GradleRunner] を返す。 */
    public fun runner(vararg arguments: String): GradleRunner {
        writeSettings()
        return GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withArguments(arguments.toList() + "--stacktrace")
            .forwardOutput()
            .apply { if (pluginClasspath.isNotEmpty()) withPluginClasspath(pluginClasspath) }
            .apply { gradleVersion?.let { withGradleVersion(it) } }
    }

    internal fun writeSettings() {
        val mirror = System.getProperty("acg.mavenMirror")?.takeIf { it.isNotBlank() }
        val mirrorLine = mirror?.let { "maven(\"$it\")" }

        fun repos(
            extra: List<String>,
            base: List<String>,
        ) = (listOfNotNull(mirrorLine) + extra + base).joinToString("\n") { "        $it" }
        file(
            "settings.gradle.kts",
            """
            |pluginManagement {
            |    repositories {
            |${repos(extraPluginRepositories, listOf("gradlePluginPortal()", "mavenCentral()", "google()"))}
            |    }
            |}
            |
            |dependencyResolutionManagement {
            |    repositories {
            |${repos(extraRepositories, listOf("mavenCentral()", "google()"))}
            |    }
            |}
            |
            |rootProject.name = "$rootProjectName"
            |${includes.joinToString("\n") { "include(\"$it\")" }}
            """.trimMargin(),
        )
        file("build.gradle.kts", buildScript)
        file("gradle.properties", gradleProperties.entries.joinToString("\n") { "${it.key}=${it.value}" })
    }

    public companion object {
        /** `acg.kotlin` か、testing が使っているコンパイラの版。 */
        public fun defaultKotlinVersion(): String =
            System.getProperty("acg.kotlin")?.takeIf { it.isNotBlank() } ?: KotlinCompilerVersion.VERSION
    }
}

/** 一時ディレクトリ（または [projectDir]）にフィクスチャを作る。 */
public fun gradleFixture(
    projectDir: File? = null,
    block: GradleFixture.() -> Unit,
): GradleFixture {
    val dir = projectDir ?: Files.createTempDirectory("acg-gradle-fixture").toFile()
    return GradleFixture(dir).apply(block)
}

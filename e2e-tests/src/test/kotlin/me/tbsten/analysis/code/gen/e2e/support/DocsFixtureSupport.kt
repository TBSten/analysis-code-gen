package me.tbsten.analysis.code.gen.e2e.support

import java.io.File

/**
 * docs のコード例の出所（fixtures/docs/<ページ>、ADR-031）を、公開前の手元の成果物でビルドするための init script（ACG-67、ADR-066）。
 *
 * 出所は利用者が書くとおりの形（公開版 `0.1.0`・Maven Central）のまま保つ。ページの例と一字一句一致させるため、
 * テスト用のリポジトリや版をフィクスチャ側には書かない。代わりにこの init script が次を足す:
 * - plugin と依存の置き場の先頭に build/fixture-repo（`./gradlew publishFixtureDeps` が作る）
 * - `me.tbsten.analysis.code.gen` の plugin と成果物の版を、リポジトリの acg.version に置き換える
 * - `-Pacg.mavenMirror` があれば、Maven Central の代わりにそのミラーを先に見る（クラウド環境向け、ADR-025）
 */
internal fun docsFixtureInitScript(dir: File): File {
    val repo = repoRoot.resolve("build/fixture-repo").invariantSeparatorsPath
    val version = docsAcgVersion()
    val mirrorRepo =
        System
            .getProperty("acg.mavenMirror")
            ?.takeIf { it.isNotBlank() }
            ?.let { "maven { url = uri(\"$it\") }" }
            .orEmpty()
    val script =
        """
        def acgRepo = { repos ->
            repos.maven {
                name = "acgFixtureRepo"
                url = uri("file://$repo")
                content { includeGroupAndSubgroups("me.tbsten.analysis.code.gen") }
            }
        }
        // フィクスチャの settings より先に足すので、ここの置き場が先に見られる
        beforeSettings { settings ->
            settings.pluginManagement.repositories { r -> acgRepo(r); $mirrorRepo }
            settings.pluginManagement.resolutionStrategy.eachPlugin {
                if (requested.id.id.startsWith("me.tbsten.analysis.code.gen")) useVersion("$version")
            }
            settings.dependencyResolutionManagement.repositories { r -> acgRepo(r); $mirrorRepo }
        }
        allprojects {
            configurations.configureEach {
                resolutionStrategy.eachDependency {
                    if (requested.group == "me.tbsten.analysis.code.gen") useVersion("$version")
                }
            }
        }
        """.trimIndent()
    return File(dir, "acg-docs-fixture.init.gradle").apply { writeText(script) }
}

/** リポジトリの acg.version（gradle.properties）。 */
private fun docsAcgVersion(): String =
    repoRoot
        .resolve("gradle.properties")
        .readLines()
        .first { it.startsWith("acg.version=") }
        .substringAfter("=")
        .trim()

// フィクスチャ共通の settings（ADR-027）。各フィクスチャの settings.gradle.kts が
//   pluginManagement { includeBuild("../build-support") }
//   plugins { id("acg.fixture-settings") }
// と書いて使う。やること:
// - リポジトリ: build/fixture-repo（./gradlew publishFixtureDeps で作る）・Maven Central・Google
// - version catalog: libs（リポジトリの gradle/libs.versions.toml）と acg（テスト用 Processor の座標）
// - plugin `me.tbsten.analysiscodegen*` の版をリポジトリの acg.version にそろえる
// - -Pacg.fixture.engine=ksp のとき、各プロジェクトのビルドスクリプトを build.ksp.gradle.kts に切り替える
//
// プロパティ:
//   acg.fixture.engine  acg（既定。公開 DSL）/ ksp（KSP2 + テスト用 Processor の KSP 版）
//   acg.repoRoot        リポジトリのルート。省略時は上へたどって探す
//   acg.fixture.repo    fixture-repo の場所。省略時は <repoRoot>/build/fixture-repo
//   acg.fixture.version テスト用 Processor と plugin の版。省略時は <repoRoot>/gradle.properties の acg.version
import java.util.Properties

val repoRoot: File =
    providers.gradleProperty("acg.repoRoot").orNull?.let(::File)
        ?: generateSequence(settingsDir) { it.parentFile }
            .firstOrNull { File(it, "fixtures/processors/processors.toml").isFile && File(it, "gradle/libs.versions.toml").isFile }
        ?: error("analysis-code-gen のリポジトリのルートが見つからない。-Pacg.repoRoot=<path> を指定する")

val engine: String = providers.gradleProperty("acg.fixture.engine").orNull ?: "acg"
require(engine == "acg" || engine == "ksp") { "acg.fixture.engine は acg か ksp: $engine" }

val acgVersion: String =
    providers.gradleProperty("acg.fixture.version").orNull
        ?: Properties().also { props -> File(repoRoot, "gradle.properties").inputStream().use(props::load) }.getProperty("acg.version")

val fixtureRepo: File = providers.gradleProperty("acg.fixture.repo").orNull?.let(::File) ?: File(repoRoot, "build/fixture-repo")

fun RepositoryHandler.fixtureRepositories() {
    maven {
        name = "fixtureRepo"
        url = fixtureRepo.toURI()
        content { includeGroupAndSubgroups("me.tbsten.analysis.code.gen") }
    }
    mavenCentral()
    google()
}

pluginManagement {
    repositories {
        fixtureRepositories()
        gradlePluginPortal()
    }
    resolutionStrategy.eachPlugin {
        if (requested.id.id.startsWith("me.tbsten.analysis.code.gen")) useVersion(acgVersion)
    }
}

dependencyResolutionManagement {
    // KGP が Node.js などの配布元をプロジェクトに足すため、リポジトリモードは既定（プロジェクト側も許す）にする
    repositories {
        fixtureRepositories()
    }
    versionCatalogs {
        create("libs") {
            from(files(File(repoRoot, "gradle/libs.versions.toml")))
        }
        create("acg") {
            version("acg", acgVersion)
            library("testProcessors-native", "me.tbsten.analysis.code.gen", "test-processors-native").versionRef("acg")
            library("testProcessors-ksp", "me.tbsten.analysis.code.gen", "test-processors-ksp").versionRef("acg")
        }
    }
}

if (engine == "ksp") {
    gradle.settingsEvaluated {
        fun ProjectDescriptor.useKspBuildFile() {
            buildFileName = "build.ksp.gradle.kts"
            children.forEach { it.useKspBuildFile() }
        }
        rootProject.useKspBuildFile()
    }
}

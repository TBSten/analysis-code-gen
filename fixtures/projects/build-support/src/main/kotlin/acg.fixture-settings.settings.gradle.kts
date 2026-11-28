// フィクスチャ共通の settings（ADR-027）。各フィクスチャの settings.gradle.kts が
//   pluginManagement { includeBuild("../build-support") }
//   plugins { id("acg.fixture-settings") }
// と書いて使う。やること:
// - リポジトリ: build/fixture-repo（./gradlew publishFixtureDeps で作る）・Maven Central・Google
// - version catalog: libs（リポジトリの gradle/libs.versions.toml）と acg（テスト用 Processor の座標）
// - plugin `me.tbsten.analysis.code.gen*` の版をリポジトリの acg.version にそろえる
// - version catalog: rw（実用 Processor のサンプル fixtures/real-world の座標。ADR-019・037）
// - -Pacg.fixture.engine=ksp のとき、各プロジェクトのビルドスクリプトを build.ksp.gradle.kts に、
//   bridge のとき build.bridge.gradle.kts に切り替える（ADR-037）
//
// プロパティ:
//   acg.fixture.engine  acg（既定。公開 DSL）/ native（acg と同じ。実用 Processor の Native 書き直し版）/
//                       bridge（公開 DSL + KSP の Processor を Bridge で）/ ksp（KSP2 + テスト用 Processor の KSP 版）
//   acg.mavenMirror     Maven Central のミラー。あれば最初に見る（429 を返すクラウド環境向け。ADR-025）
//   acg.processor.<ID>.version  実用 Processor の版。省略時は <repoRoot>/fixtures/processors/processors.toml の値
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
require(engine in setOf("acg", "native", "bridge", "ksp")) { "acg.fixture.engine は acg / native / bridge / ksp のどれか: $engine" }

val acgVersion: String =
    providers.gradleProperty("acg.fixture.version").orNull
        ?: Properties().also { props -> File(repoRoot, "gradle.properties").inputStream().use(props::load) }.getProperty("acg.version")

val fixtureRepo: File = providers.gradleProperty("acg.fixture.repo").orNull?.let(::File) ?: File(repoRoot, "build/fixture-repo")

// 実用 Processor の版（ADR-019）。テストは -Pacg.processor.<ID>.version で渡す。手で動かすときは processors.toml から読む
val processorVersionsInToml: Map<String, String> =
    File(repoRoot, "fixtures/processors/processors.toml")
        .readText()
        .split("[[processor]]")
        .drop(1)
        .associate { block ->
            fun value(key: String) =
                Regex("""(?m)^$key\s*=\s*"([^"]*)"""").find(block)?.groupValues?.get(1)
                    ?: error("processors.toml の $key が無い: $block")
            value("id") to value("version")
        }

fun processorVersion(id: String): String =
    providers.gradleProperty("acg.processor.$id.version").orNull
        ?: processorVersionsInToml[id]
        ?: error("processors.toml に $id が無い")

val mavenMirror: String? = providers.gradleProperty("acg.mavenMirror").orNull?.takeIf { it.isNotBlank() }

fun RepositoryHandler.fixtureRepositories() {
    mavenMirror?.let { mirror -> maven { url = uri(mirror) } }
    maven {
        name = "fixtureRepo"
        url = fixtureRepo.toURI()
        content { includeGroupAndSubgroups("me.tbsten.analysis.code.gen") }
    }
    mavenCentral()
    google()
    // Analysis API と IntelliJ Platform（plugin が Worker で使う。docs のインストールと同じ。ADR-063）
    maven {
        url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
        content {
            includeModuleByRegex("org\\.jetbrains\\.kotlin", ".*-for-ide")
            includeGroupByRegex("org\\.jetbrains\\.intellij\\.deps.*")
        }
    }
    maven {
        url = uri("https://www.jetbrains.com/intellij-repository/releases")
        content {
            includeGroup("com.jetbrains.intellij.platform")
            includeGroup("com.jetbrains.intellij.java")
        }
    }
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
        // 実用 Processor のサンプル（fixtures/real-world、ADR-037）。元の Processor・その実行時ライブラリ・Native 書き直し版
        create("rw") {
            version("acg", acgVersion)
            version("cream", processorVersion("CRM"))
            version("moshi", processorVersion("MSH"))
            version("koin", processorVersion("KOI"))
            version("kotlinInject", processorVersion("KIN"))
            library("cream-runtime", "me.tbsten.cream", "cream-runtime").versionRef("cream")
            library("cream-ksp", "me.tbsten.cream", "cream-ksp").versionRef("cream")
            library("cream-native", "me.tbsten.analysis.code.gen", "real-world-ports-cream").versionRef("acg")
            library("moshi-runtime", "com.squareup.moshi", "moshi").versionRef("moshi")
            library("moshi-codegen", "com.squareup.moshi", "moshi-kotlin-codegen").versionRef("moshi")
            library("koin-annotations", "io.insert-koin", "koin-annotations").versionRef("koin")
            library("koin-compiler", "io.insert-koin", "koin-ksp-compiler").versionRef("koin")
            // koin-ksp-compiler 2.3.1 の pom が依存する koin-core の版。Processor ではないので processors.toml には持たない
            library("koin-core", "io.insert-koin", "koin-core").version("4.1.0")
            library("kotlinInject-runtime", "me.tatarka.inject", "kotlin-inject-runtime").versionRef("kotlinInject")
            library("kotlinInject-compiler", "me.tatarka.inject", "kotlin-inject-compiler-ksp").versionRef("kotlinInject")
        }
    }
}

val engineBuildFile: String? =
    when (engine) {
        "ksp" -> "build.ksp.gradle.kts"
        "bridge" -> "build.bridge.gradle.kts"
        else -> null
    }
if (engineBuildFile != null) {
    gradle.settingsEvaluated {
        fun ProjectDescriptor.useEngineBuildFile() {
            // 既定のビルドスクリプトを持つプロジェクトだけ差し替える。差し替え先が無いのは書き忘れなので止める
            if (File(projectDir, "build.gradle.kts").isFile) {
                check(File(projectDir, engineBuildFile).isFile) {
                    "acg.fixture.engine=$engine には ${projectDir.name}/$engineBuildFile が要る（ADR-027・037）"
                }
                buildFileName = engineBuildFile
            }
            children.forEach { it.useEngineBuildFile() }
        }
        rootProject.useEngineBuildFile()
    }
}

pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        // Analysis API の *-for-ide 成果物（ADR-011）。stable の各版（2.4.0, 2.4.10, 2.4.21 など）はここにある
        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") {
            content {
                includeGroup("org.jetbrains.kotlin")
                includeGroupByRegex("org\\.jetbrains\\.intellij\\.deps.*")
            }
        }
        // AA が前提とする IntelliJ Platform
        maven("https://www.jetbrains.com/intellij-repository/releases") {
            content {
                includeGroup("com.jetbrains.intellij.platform")
                includeGroup("com.jetbrains.intellij.java")
            }
        }
    }
}

rootProject.name = "analysis-code-gen"

// モジュール構成は設計文書 16 章の案に ACG-34 の決め打ち（analysis-code-gen-engine-spi の追加、
// analysis-code-gen-ksp-bridge → analysis-code-gen-engine-spi）を反映。境界の確定は ADR-012〜017。名前は ADR-042
include(
    ":analysis-code-gen-processor-api",
    ":analysis-code-gen-project-model",
    ":analysis-code-gen-engine-spi",
    ":analysis-code-gen-analysis-engine",
    ":analysis-code-gen-processing-engine",
    ":analysis-code-gen-incremental-engine",
    ":analysis-code-gen-gradle-plugin",
    ":analysis-code-gen-cli",
    ":analysis-code-gen-testing",
    ":analysis-code-gen-testing-kct",
    ":benchmarks",
    ":analysis-code-gen-ksp-bridge",
    ":analysis-code-gen-ksp-bridge-api",
    ":ksp-compatibility-tests",
    ":e2e-tests",
    // AA の型に触れる I / U の保留テスト（ACG-45、ADR-034）。e2e-tests は AA なしでコンパイルできるままにする
    ":engine-tests",
    // 三者差分ハーネス（ACG-42、ADR-023）
    ":differential",
    // テスト基盤（ACG-37、ADR-030）。:quality はケース ID 整合チェックを持つ（quality/ 直下）
    ":test-foundation",
    ":quality",
    // PBT 基盤（ACG-40、ADR-022）
    ":pbt",
    // テスト用 Processor（ADR-027）
    ":test-processors:core",
    ":test-processors:ksp",
    ":test-processors:native",
    // 実用 Processor の Native 書き直し版（ACG-66、ADR-037・071）
    ":real-world-ports:cream",
)


// ADR-042: 公開しうるモジュールは `analysis-code-gen-` で始める（group は me.tbsten.analysis.code.gen）。
// 公開しない検査用・テスト用のモジュールだけを例外にする。新しい内部モジュールを足すときはここにも足す
val internalOnlyModules = setOf(
    "benchmarks",
    "ksp-compatibility-tests",
    "e2e-tests",
    "engine-tests",
    "differential",
    "test-foundation",
    "quality",
    "pbt",
    "test-processors",
    "real-world-ports",
    // katachi のアーキテクチャ検査（ACG-50、ADR-041）
    "architecture-test",
)
rootProject.children.forEach { module ->
    check(module.name.startsWith("analysis-code-gen-") || module.name in internalOnlyModules) {
        "モジュール ${module.path} の名前は analysis-code-gen- で始める（内部専用なら settings.gradle.kts の internalOnlyModules に足す。ADR-042）"
    }
    check(module.projectDir.name == module.name) { "モジュール ${module.path} のディレクトリ名をモジュール名と同じにする（ADR-042）" }
}

include(":architecture-test")

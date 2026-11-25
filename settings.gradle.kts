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

// モジュール構成は設計文書 16 章の案に ACG-34 の決め打ち（engine-spi の追加、ksp-bridge → engine-spi）を反映。境界の確定は ADR-012〜017
include(
    ":processor-api",
    ":project-model",
    ":engine-spi",
    ":analysis-engine",
    ":processing-engine",
    ":incremental-engine",
    ":gradle-plugin",
    ":cli",
    ":testing",
    ":testing-kct",
    ":benchmarks",
    ":ksp-bridge",
    ":ksp-bridge-api",
    ":ksp-bridge-gradle-plugin",
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
)


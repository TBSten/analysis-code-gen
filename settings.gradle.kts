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
        // Analysis API の *-for-ide 成果物（ADR-011）
        maven("https://redirector.kotlinlang.org/maven/kotlin-ide-plugin-dependencies") {
            content { includeGroup("org.jetbrains.kotlin") }
        }
        // AA が前提とする IntelliJ Platform
        maven("https://www.jetbrains.com/intellij-repository/releases") {
            content {
                includeGroup("com.jetbrains.intellij.platform")
                includeGroup("com.jetbrains.intellij.java")
            }
        }
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies") {
            content { includeGroupByRegex("org\\.jetbrains\\.intellij\\.deps.*") }
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
    ":benchmarks",
    ":ksp-bridge",
    ":ksp-bridge-api",
    ":ksp-bridge-gradle-plugin",
    ":ksp-compatibility-tests",
    ":e2e-tests",
)

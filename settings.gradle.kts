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
    // テスト基盤（ACG-37、ADR-030）。:quality はケース ID 整合チェックを持つ（quality/ 直下）
    ":test-foundation",
    ":quality",
)

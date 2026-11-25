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
    ":testing-kct",
    ":benchmarks",
    ":ksp-bridge",
    ":ksp-bridge-api",
    ":ksp-bridge-gradle-plugin",
    ":ksp-compatibility-tests",
    ":e2e-tests",
    // AA の型に触れる I / U の保留テスト（ACG-45、ADR-034）。e2e-tests は AA なしでコンパイルできるままにする
    ":engine-tests",
    // テスト基盤（ACG-37、ADR-030）。:quality はケース ID 整合チェックを持つ（quality/ 直下）
    ":test-foundation",
    ":quality",
    // PBT 基盤（ACG-40、ADR-022）
    ":pbt",
    // テスト用 Processor（ADR-027）
    ":test-processors:core",
    ":test-processors:ksp",
)

// TP-* の Native 版は processor-api のシグネチャ（ACG-34）と AA の依存（acg.analysis-api）が main に入るまで既定では外す。
// 入ったら既定で含める（ADR-027）
if (providers.gradleProperty("acg.nativeTestProcessors").orNull == "true") {
    include(":test-processors:native")
}

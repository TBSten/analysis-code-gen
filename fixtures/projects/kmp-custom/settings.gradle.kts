// F-KMP-CUSTOM: デフォルト階層テンプレートを使わず dependsOn を手で組んだ階層（ADR-027）
pluginManagement {
    includeBuild("../build-support")
}

plugins {
    id("acg.fixture-settings")
}

rootProject.name = "fixture-kmp-custom"

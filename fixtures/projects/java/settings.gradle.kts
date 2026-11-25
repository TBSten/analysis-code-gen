// F-JAVA: Kotlin + Java 混在。Kotlin の解析で Java の宣言を解決する（ADR-027）
pluginManagement {
    includeBuild("../build-support")
}

plugins {
    id("acg.fixture-settings")
}

rootProject.name = "fixture-java"


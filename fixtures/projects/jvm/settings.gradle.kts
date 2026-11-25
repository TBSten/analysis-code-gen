// F-JVM: Kotlin/JVM 単一モジュール（main + test）。ADR-027
pluginManagement {
    includeBuild("../build-support")
}

plugins {
    id("acg.fixture-settings")
}

rootProject.name = "fixture-jvm"

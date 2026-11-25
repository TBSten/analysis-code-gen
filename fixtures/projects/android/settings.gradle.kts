// F-AND: Android アプリ（:app）+ ライブラリ（:lib）。debug/release × free/paid の 4 Variant、unit test と androidTest あり（ADR-027）
pluginManagement {
    includeBuild("../build-support")
}

plugins {
    id("acg.fixture-settings")
}

rootProject.name = "fixture-android"
include(":lib", ":app")

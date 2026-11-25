// F-KMP: jvm・android・iosArm64・iosSimulatorArm64・linuxX64・js(IR)・wasmJs。デフォルト階層テンプレート（ADR-027）
pluginManagement {
    includeBuild("../build-support")
}

plugins {
    id("acg.fixture-settings")
}

rootProject.name = "fixture-kmp"

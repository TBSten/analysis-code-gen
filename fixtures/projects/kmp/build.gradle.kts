// 公開 DSL（ADR-016）で書いた F-KMP。同じソースを KSP2 で動かす版は build.ksp.gradle.kts
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    id("me.tbsten.analysiscodegen")
}

kotlin {
    jvm()
    androidLibrary {
        namespace = "fixture.kmp"
        compileSdk = 36
        minSdk = 24
    }
    iosArm64()
    iosSimulatorArm64()
    linuxX64()
    js { nodejs() }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { nodejs() }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

analysisCodeGen {
    // commonMain に 1 回だけ生成し、全ターゲットから使う（KMP-01）
    sourceSets {
        commonMain {
            processor(acg.testProcessors.native)
            arg("tp.processors", "Mapper,Chain,Defer")
        }
    }
    // Compilation ごとに actual と集約を作る（KMP-03・13）
    compilations {
        all {
            processor(acg.testProcessors.native)
            arg("tp.processors", "Actual,Registry")
        }
    }
}

// 公開 DSL で書いた F-AND の :lib
plugins {
    alias(libs.plugins.android.library)
    id("me.tbsten.analysis.code.gen")
}

android {
    namespace = "fixture.android.lib"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

analysisCodeGen {
    arg("tp.processors", "Mapper")
    compilations {
        all { processor(acg.testProcessors.native) }
    }
}

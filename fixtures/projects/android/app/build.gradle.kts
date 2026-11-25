// 公開 DSL（ADR-016）で書いた F-AND の :app。Android は Variant を Compilation として扱う（ADR-016）。Kotlin は AGP の組み込み Kotlin
plugins {
    alias(libs.plugins.android.application)
    id("me.tbsten.analysis.code.gen")
}

android {
    namespace = "fixture.android.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "fixture.android.app"
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    flavorDimensions += "tier"
    productFlavors {
        create("free") { dimension = "tier" }
        create("paid") { dimension = "tier" }
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
    arg("tp.processors", "Mapper,Registry")
    compilations {
        all { processor(acg.testProcessors.native) }
    }
}

dependencies {
    implementation(project(":lib"))
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

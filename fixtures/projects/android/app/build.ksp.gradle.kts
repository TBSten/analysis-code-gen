// F-AND の :app を KSP2 でビルドする版（-Pacg.fixture.engine=ksp）
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
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

ksp {
    arg("tp.processors", "Mapper,Registry")
}

dependencies {
    implementation(project(":lib"))
    ksp(acg.testProcessors.ksp)
    kspTest(acg.testProcessors.ksp)
    kspAndroidTest(acg.testProcessors.ksp)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

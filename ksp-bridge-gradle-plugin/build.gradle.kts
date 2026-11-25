plugins {
    id("acg.gradle-plugin")
    id("acg.dokka")
}

dependencies {
    implementation(project(":gradle-plugin"))
}

gradlePlugin {
    plugins {
        // ADR-016・017
        create("analysisCodegenKspBridge") {
            id = "me.tbsten.analysiscodegen.ksp-bridge"
            implementationClass = "me.tbsten.analysiscodegen.ksp.gradle.KspBridgePlugin"
        }
    }
}

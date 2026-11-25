plugins {
    id("acg.gradle-plugin")
    id("acg.dokka")
}

dependencies {
    implementation(project(":analysis-code-gen-gradle-plugin"))
}

gradlePlugin {
    plugins {
        // ADR-016・017
        create("analysisCodegenKspBridge") {
            id = "me.tbsten.analysis.code.gen.ksp-bridge"
            implementationClass = "me.tbsten.analysis.code.gen.ksp.gradle.KspBridgePlugin"
        }
    }
}

plugins {
    id("acg.gradle-plugin")
    id("acg.dokka")
}

dependencies {
    implementation(project(":gradle-plugin"))
}

gradlePlugin {
    plugins {
        create("analysisCodegenKspBridge") {
            id = "me.tbsten.analysiscodegen.ksp-bridge"
            implementationClass = "me.tbsten.analysiscodegen.kspbridgegradleplugin.KspBridgeGradlePlugin"
        }
    }
}

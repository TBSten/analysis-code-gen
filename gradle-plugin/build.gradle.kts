plugins {
    id("acg.gradle-plugin")
    id("acg.dokka")
}

dependencies {
    implementation(project(":project-model"))
}

gradlePlugin {
    plugins {
        // プラグイン ID は ACG-34（Gradle DSL の設計）で確定する
        create("analysisCodegen") {
            id = "me.tbsten.analysiscodegen"
            implementationClass = "me.tbsten.analysiscodegen.gradleplugin.AnalysisCodegenGradlePlugin"
        }
    }
}

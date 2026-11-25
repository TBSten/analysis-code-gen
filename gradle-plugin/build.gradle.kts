plugins {
    id("acg.gradle-plugin")
    id("acg.dokka")
    id("acg.fixture-publish")
}

// ADR-012: ビルドスクリプトの classpath に載るのは project-model と Gradle 連携だけ。エンジンと AA は Worker 側で読む
dependencies {
    implementation(project(":project-model"))
}

gradlePlugin {
    plugins {
        // ADR-016
        create("analysisCodegen") {
            id = "me.tbsten.analysiscodegen"
            implementationClass = "me.tbsten.analysiscodegen.gradle.AnalysisCodegenPlugin"
        }
    }
}

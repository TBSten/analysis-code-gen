plugins {
    id("acg.library")
    id("acg.analysis-api")
}

dependencies {
    api(project(":analysis-code-gen-project-model"))
    // InternalAnalysisCodegenApi の印だけを使う
    implementation(project(":analysis-code-gen-processor-api"))
}

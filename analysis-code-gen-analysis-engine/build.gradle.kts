plugins {
    id("acg.library")
    id("acg.analysis-api")
    id("acg.fixture-publish")
}

dependencies {
    api(project(":analysis-code-gen-project-model"))
    // InternalAnalysisCodegenApi の印だけを使う
    implementation(project(":analysis-code-gen-processor-api"))
}

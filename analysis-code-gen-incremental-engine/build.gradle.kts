plugins {
    id("acg.library")
    id("acg.fixture-publish")
}

dependencies {
    api(project(":analysis-code-gen-project-model"))
    // InternalAnalysisCodegenApi の印だけを使う（AA には依存しない。ADR-012）
    implementation(project(":analysis-code-gen-processor-api"))
}

plugins {
    id("acg.library")
}

dependencies {
    api(project(":project-model"))
    // InternalAnalysisCodegenApi の印だけを使う（AA には依存しない。ADR-012）
    implementation(project(":processor-api"))
}

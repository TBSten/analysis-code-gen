plugins {
    id("acg.library")
    id("acg.analysis-api")
}

dependencies {
    api(project(":project-model"))
    // InternalAnalysisCodegenApi の印だけを使う
    implementation(project(":processor-api"))
}

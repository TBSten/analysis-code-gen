plugins {
    id("acg.library")
    id("acg.analysis-api")
    id("acg.dokka")
    id("acg.fixture-publish")
}

dependencies {
    api(project(":analysis-code-gen-processor-api"))
    api(libs.ksp.api)
}

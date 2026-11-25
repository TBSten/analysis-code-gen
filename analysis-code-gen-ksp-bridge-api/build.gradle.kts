plugins {
    id("acg.library")
    id("acg.analysis-api")
    id("acg.dokka")
}

dependencies {
    api(project(":analysis-code-gen-processor-api"))
    api(libs.ksp.api)
}

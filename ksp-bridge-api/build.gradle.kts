plugins {
    id("acg.library")
    id("acg.analysis-api")
    id("acg.dokka")
}

dependencies {
    api(project(":processor-api"))
    api(libs.ksp.api)
}

plugins {
    id("acg.library")
    id("acg.analysis-api")
}

dependencies {
    api(project(":processor-api"))
    api(libs.ksp.api)
}

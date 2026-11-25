plugins {
    id("acg.library")
}

dependencies {
    api(project(":processor-api"))
    api(libs.ksp.api)
}

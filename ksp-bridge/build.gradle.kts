plugins {
    id("acg.library")
}

dependencies {
    api(project(":ksp-bridge-api"))
    implementation(project(":engine-spi"))
    implementation(project(":processor-api"))
    implementation(libs.ksp.api)
}

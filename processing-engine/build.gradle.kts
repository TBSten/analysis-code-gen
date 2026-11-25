plugins {
    id("acg.library")
}

dependencies {
    api(project(":processor-api"))
    api(project(":engine-spi"))
    implementation(project(":analysis-engine"))
    implementation(project(":incremental-engine"))
    implementation(project(":project-model"))
}

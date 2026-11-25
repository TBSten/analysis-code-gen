plugins {
    id("acg.library")
    id("acg.analysis-api")
}

dependencies {
    api(project(":processor-api"))
    api(project(":engine-spi"))
    api(project(":analysis-engine"))
    api(project(":incremental-engine"))
    api(project(":project-model"))
}

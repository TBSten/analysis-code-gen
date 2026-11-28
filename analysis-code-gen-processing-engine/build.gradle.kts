plugins {
    id("acg.library")
    id("acg.analysis-api")
    id("acg.fixture-publish")
}

dependencies {
    api(project(":analysis-code-gen-processor-api"))
    api(project(":analysis-code-gen-engine-spi"))
    api(project(":analysis-code-gen-analysis-engine"))
    api(project(":analysis-code-gen-incremental-engine"))
    api(project(":analysis-code-gen-project-model"))
}

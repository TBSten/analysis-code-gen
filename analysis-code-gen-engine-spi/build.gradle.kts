plugins {
    id("acg.library")
    id("acg.analysis-api")
    id("acg.fixture-publish")
}

dependencies {
    api(project(":analysis-code-gen-processor-api"))
    api(project(":analysis-code-gen-project-model"))
}

plugins {
    id("acg.library")
    id("acg.analysis-api")
}

dependencies {
    api(project(":analysis-code-gen-processor-api"))
    api(project(":analysis-code-gen-project-model"))
}

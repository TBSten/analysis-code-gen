plugins {
    id("acg.jvm")
    id("acg.analysis-api")
}

dependencies {
    implementation(project(":analysis-code-gen-processing-engine"))
}

plugins {
    id("acg.jvm")
    id("acg.analysis-api")
}

dependencies {
    implementation(project(":processing-engine"))
}

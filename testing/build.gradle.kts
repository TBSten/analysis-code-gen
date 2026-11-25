plugins {
    id("acg.library")
}

dependencies {
    api(project(":processor-api"))
    implementation(project(":processing-engine"))
}

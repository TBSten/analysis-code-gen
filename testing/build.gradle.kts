plugins {
    id("acg.library")
    id("acg.dokka")
}

dependencies {
    api(project(":processor-api"))
    implementation(project(":processing-engine"))
}

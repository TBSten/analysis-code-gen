plugins {
    id("acg.jvm")
}

dependencies {
    testImplementation(project(":ksp-bridge"))
    testImplementation(project(":testing"))
}

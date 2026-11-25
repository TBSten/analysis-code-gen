plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

ksp {
    arg("tp.processors", "Mapper,Resource,Defer")
}

dependencies {
    ksp(acg.testProcessors.ksp)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

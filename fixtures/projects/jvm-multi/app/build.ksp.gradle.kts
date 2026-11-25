plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

ksp {
    arg("tp.processors", "Mapper,Registry,Defer")
    arg("tp.registry.name", "AppRegistry")
    arg("tp.registry.package", "fixture.multi.app.generated")
}

dependencies {
    implementation(project(":lib"))
    ksp(acg.testProcessors.ksp)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

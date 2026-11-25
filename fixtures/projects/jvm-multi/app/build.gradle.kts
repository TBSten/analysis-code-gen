plugins {
    alias(libs.plugins.kotlin.jvm)
    id("me.tbsten.analysiscodegen")
}

analysisCodeGen {
    arg("tp.processors", "Mapper,Registry,Defer")
    arg("tp.registry.name", "AppRegistry")
    arg("tp.registry.package", "fixture.multi.app.generated")
    sourceSets {
        main { processor(acg.testProcessors.native) }
    }
}

dependencies {
    implementation(project(":lib"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

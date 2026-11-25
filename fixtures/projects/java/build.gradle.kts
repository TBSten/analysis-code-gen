plugins {
    alias(libs.plugins.kotlin.jvm)
    id("me.tbsten.analysiscodegen")
}

analysisCodeGen {
    arg("tp.processors", "Mapper,Resource,Defer")
    sourceSets {
        main { processor(acg.testProcessors.native) }
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("me.tbsten.analysis.code.gen")
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

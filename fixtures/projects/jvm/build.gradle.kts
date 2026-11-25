// 公開 DSL（ADR-016）で書いた F-JVM。同じソースを KSP2 で動かす版は build.ksp.gradle.kts
plugins {
    alias(libs.plugins.kotlin.jvm)
    id("me.tbsten.analysis.code.gen")
}

analysisCodeGen {
    arg("tp.processors", "Mapper,Registry,Chain,Body,Defer,Resource")
    sourceSets {
        main { processor(acg.testProcessors.native) }
        test { processor(acg.testProcessors.native) }
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

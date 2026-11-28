// N: analysisCodeGen + cream.kt の Native 書き直し版（real-world-ports:cream、ADR-037）
plugins {
    alias(libs.plugins.kotlin.jvm)
    id("me.tbsten.analysis.code.gen")
}

analysisCodeGen {
    sourceSets {
        main { processor(rw.cream.native) }
    }
}

dependencies {
    implementation(rw.cream.runtime)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

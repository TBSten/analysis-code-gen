// B: analysisCodeGen の Bridge + 元の Moshi codegen（ADR-050）
plugins {
    alias(libs.plugins.kotlin.jvm)
    id("me.tbsten.analysis.code.gen")
}

analysisCodeGen {
    sourceSets {
        main { processor(rw.moshi.codegen) }
    }
}

dependencies {
    implementation(rw.moshi.runtime)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

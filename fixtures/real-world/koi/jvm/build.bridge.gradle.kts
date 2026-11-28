// B: analysisCodeGen の Bridge + 元の Koin Annotations の KSP Processor（ADR-050）
plugins {
    alias(libs.plugins.kotlin.jvm)
    id("me.tbsten.analysis.code.gen")
}

analysisCodeGen {
    sourceSets {
        main { processor(rw.koin.compiler) }
    }
}

dependencies {
    implementation(rw.koin.core)
    implementation(rw.koin.annotations)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

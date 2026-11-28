// B: analysisCodeGen の Bridge + 元の cream.kt（KSP の Processor なので Bridge が自動で有効になる。ADR-050）
plugins {
    alias(libs.plugins.kotlin.jvm)
    id("me.tbsten.analysis.code.gen")
}

analysisCodeGen {
    sourceSets {
        main { processor(rw.cream.ksp) }
    }
}

dependencies {
    implementation(rw.cream.runtime)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

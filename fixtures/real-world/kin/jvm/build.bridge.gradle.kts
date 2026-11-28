// B: analysisCodeGen の Bridge + 元の kotlin-inject（ADR-050）
plugins {
    alias(libs.plugins.kotlin.jvm)
    id("me.tbsten.analysis.code.gen")
}

analysisCodeGen {
    sourceSets {
        main { processor(rw.kotlinInject.compiler) }
    }
}

dependencies {
    implementation(rw.kotlinInject.runtime)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

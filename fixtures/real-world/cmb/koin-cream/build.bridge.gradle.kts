// B: analysisCodeGen の Bridge + 元の Koin Annotations と cream.kt（ADR-050）
plugins {
    alias(libs.plugins.kotlin.jvm)
    id("me.tbsten.analysis.code.gen")
}

analysisCodeGen {
    sourceSets {
        main {
            processor(rw.koin.compiler)
            processor(rw.cream.ksp)
        }
    }
}

dependencies {
    implementation(rw.koin.core)
    implementation(rw.koin.annotations)
    implementation(rw.cream.runtime)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

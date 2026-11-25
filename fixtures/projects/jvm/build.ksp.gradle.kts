// F-JVM を KSP2 + テスト用 Processor の KSP 版でビルドする版（-Pacg.fixture.engine=ksp）。TP-Body は KSP 版が無い
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

ksp {
    arg("tp.processors", "Mapper,Registry,Chain,Defer,Resource")
}

dependencies {
    ksp(acg.testProcessors.ksp)
    kspTest(acg.testProcessors.ksp)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

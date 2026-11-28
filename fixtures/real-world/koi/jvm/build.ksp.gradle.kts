// K: 公式 KSP2 + 元の Koin Annotations の KSP Processor（比較の基準）
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(rw.koin.core)
    implementation(rw.koin.annotations)
    ksp(rw.koin.compiler)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

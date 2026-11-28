// K: 公式 KSP2 + 元の Koin Annotations と cream.kt（比較の基準）
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(rw.koin.core)
    implementation(rw.koin.annotations)
    implementation(rw.cream.runtime)
    ksp(rw.koin.compiler)
    ksp(rw.cream.ksp)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

// K: 公式 KSP2 + 元の kotlin-inject（比較の基準）
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(rw.kotlinInject.runtime)
    ksp(rw.kotlinInject.compiler)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

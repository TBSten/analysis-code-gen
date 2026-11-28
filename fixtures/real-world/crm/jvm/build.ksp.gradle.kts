// K: 公式 KSP2 + 元の cream.kt（比較の基準）
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(rw.cream.runtime)
    ksp(rw.cream.ksp)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

// K: 公式 KSP2 + 元の Moshi codegen（比較の基準）
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(rw.moshi.runtime)
    ksp(rw.moshi.codegen)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

// kotlin-compile-testing の fork（ZacSweers/kotlin-compile-testing 0.14.0、MPL-2.0）。ADR-021
// - ソースは porting/kct/vendor.py で取り込む。手で直したら Modifications 行に書く（LIC-01）
// - ライセンスの境界をはっきりさせるため、Apache-2.0 の testing とは別モジュールにしている
// - Kotlin コンパイラの版は -Pacg.kotlin=<版>（ADR-011）で差し替える。省略時は version catalog の kotlin
plugins {
    id("acg.jvm")
    `java-library`
}

val libs = versionCatalogs.named("libs")
val kotlinCompilerVersion: String =
    providers.gradleProperty("acg.kotlin").orNull ?: libs.findVersion("kotlin").get().requiredVersion

dependencies {
    api("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinCompilerVersion")
    api("org.jetbrains.kotlin:kotlin-annotation-processing-embeddable:$kotlinCompilerVersion")
    api(libs.findLibrary("ksp-api").get())
    implementation(libs.findLibrary("ksp-impl").get())
    implementation(libs.findLibrary("ksp-common-deps").get())
    implementation(libs.findLibrary("ksp-aa-embeddable").get())
    implementation(libs.findLibrary("okio").get())
    implementation(libs.findLibrary("classgraph").get())
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        // 取り込んだコードの警告は上流の書き方のまま残す
        allWarningsAsErrors.set(false)
    }
}

// 取り込んだコードは上流との差分を小さく保つため整形しない
tasks.matching { it.name.startsWith("ktlint") }.configureEach { enabled = false }

tasks.withType<Jar>().configureEach {
    // MPL-2.0 の本文を成果物に同梱する
    from(rootProject.file("porting/kct/LICENSE-MPL-2.0.txt")) { into("META-INF") }
}

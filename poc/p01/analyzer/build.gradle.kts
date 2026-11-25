plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    application
}

// KSP2 が Maven Central に出している AA 同梱 JAR（パッケージは ksp.org.jetbrains.* に再配置済み）。
// JetBrains の intellij-dependencies リポジトリに頼らず Standalone AA を使える唯一の経路。
val kspVersion = "2.3.14"

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-aa-embeddable:$kspVersion")
    implementation("com.google.devtools.ksp:symbol-processing-common-deps:$kspVersion")
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.addAll(
            "ksp.org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
            "ksp.org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
            "ksp.org.jetbrains.kotlin.analysis.api.KaPlatformInterface",
        )
    }
}

application {
    mainClass.set("p01.MainKt")
    applicationDefaultJvmArgs = listOf("-Xmx3g")
}

plugins {
    kotlin("jvm") version "2.4.21"
    application
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
            "-opt-in=org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
            "-opt-in=org.jetbrains.kotlin.analysis.api.KaPlatformInterface",
            "-opt-in=org.jetbrains.kotlin.analysis.api.KaIdeApi",
            "-opt-in=org.jetbrains.kotlin.analysis.api.KaNonPublicApi",
            "-opt-in=org.jetbrains.kotlin.analysis.api.permissions.KaAllowProhibitedAnalyzeFromWriteAction",
            "-Xsuppress-version-warnings",
        )
    }
}

// KSP2 が同梱する Standalone Analysis API（ksp. 接頭辞で relocate 済み）。
// JetBrains の AA リポジトリに届かない環境でも動かせるよう、relocate を戻して使う。PoC 専用（ADR-010）。
val kspVersion = "2.3.14"
val aaEmbeddable: Configuration by configurations.creating { isTransitive = false }

// 計測用の「よくある JVM / Android アプリ」相当の依存。解析対象のライブラリとして AA に渡す（PoC 本体は使わない）。
val benchLibs: Configuration by configurations.creating

dependencies {
    aaEmbeddable("com.google.devtools.ksp:symbol-processing-aa-embeddable:$kspVersion")
    implementation(files(layout.buildDirectory.file("aa/aa-unrelocated.jar")).builtBy("unrelocateAa"))
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
    implementation("com.google.devtools.ksp:symbol-processing-common-deps:$kspVersion")

    listOf(
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3",
        "org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.6.0",
        "com.squareup.okhttp3:okhttp:4.12.0",
        "com.squareup.retrofit2:retrofit:2.11.0",
        "com.squareup.moshi:moshi-kotlin:1.15.1",
        "com.squareup:kotlinpoet-jvm:1.18.1",
        "com.google.guava:guava:33.2.1-jre",
        "com.google.dagger:dagger:2.51.1",
        "io.insert-koin:koin-core-jvm:3.5.6",
        "io.ktor:ktor-client-core-jvm:2.3.12",
        "com.fasterxml.jackson.core:jackson-databind:2.17.2",
        "io.arrow-kt:arrow-core-jvm:1.2.4",
        "org.jetbrains.exposed:exposed-core:0.52.0",
        "org.slf4j:slf4j-api:2.0.13",
    ).forEach { benchLibs(it) }
}

val unrelocateAa by tasks.registering(Exec::class) {
    val input = aaEmbeddable
    val script = file("scripts/unrelocate.py")
    val output = layout.buildDirectory.file("aa/aa-unrelocated.jar")
    inputs.files(input)
    inputs.file(script)
    outputs.file(output)
    doFirst { output.get().asFile.parentFile.mkdirs() }
    commandLine("python3", script.absolutePath, input.singleFile.absolutePath, output.get().asFile.absolutePath)
}

application {
    mainClass.set("poc.p02.MainKt")
    applicationDefaultJvmArgs = listOf("-Xmx4g", "-Djava.awt.headless=true")
}

tasks.named<JavaExec>("run") {
    // gradle run --args="--files 200 --iterations 10 --libs realistic"
    workingDir = projectDir
    inputs.files(benchLibs)
    doFirst { systemProperty("p02.benchLibs", benchLibs.files.joinToString(File.pathSeparator)) }
}

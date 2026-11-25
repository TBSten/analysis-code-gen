import groovy.json.JsonOutput
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform") version "2.4.21"
}

// -Pp01.errors=common|shared|jvm で、その層だけにわざとエラーのファイルを足す（オラクル用）
val errorLayer = providers.gradleProperty("p01.errors").orNull

kotlin {
    jvmToolchain(21)
    compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
    jvm()
    js { nodejs() }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { nodejs() }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jvmAndJs") {
                withJvm()
                withJs()
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        val layerToSourceSet = mapOf("common" to "commonMain", "shared" to "jvmAndJsMain", "jvm" to "jvmMain")
        if (errorLayer != null) {
            val ss = layerToSourceSet.getValue(errorLayer)
            getByName(ss).kotlin.srcDir("src/errors/$errorLayer")
        }
    }
}

// AA 側（poc/p01/analyzer）に渡す SourceSet 構造と依存のダンプ
@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
fun classpaths(): Map<String, FileCollection> {
    val metadata = kotlin.targets.getByName("metadata").compilations
    return mapOf(
        "commonMain" to metadata.getByName("commonMain").compileDependencyFiles,
        "webMain" to metadata.getByName("webMain").compileDependencyFiles,
        "jvmAndJsMain" to metadata.getByName("jvmAndJsMain").compileDependencyFiles,
        "jvmMain" to kotlin.jvm().compilations.getByName("main").compileDependencyFiles,
        "jsMain" to kotlin.js().compilations.getByName("main").compileDependencyFiles,
        "wasmJsMain" to kotlin.wasmJs().compilations.getByName("main").compileDependencyFiles,
    )
}

tasks.register("dumpModel") {
    val out = layout.buildDirectory.file("p01-model.json")
    inputs.files(project.provider { classpaths().values.toList() })
    outputs.file(out)
    outputs.upToDateWhen { false }
    doLast {
        val k = project.extensions.getByType(org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension::class.java)
        fun platformOf(ss: org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet): List<String> =
            k.targets.filter { it.name != "metadata" }
                .filter { t -> t.compilations.getByName("main").allKotlinSourceSets.contains(ss) }
                .map { it.platformType.name }
                .sorted()
        val model = classpaths().map { (name, cp) ->
            val ss = k.sourceSets.getByName(name)
            mapOf(
                "name" to name,
                "platforms" to platformOf(ss),
                "sourceRoots" to ss.kotlin.srcDirs.filter { it.exists() }.map { it.absolutePath },
                "dependsOn" to ss.dependsOn.map { it.name },
                "classpath" to cp.files.filter { it.exists() }.map { it.absolutePath },
            )
        }
        out.get().asFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(mapOf(
            "jdkHome" to System.getProperty("java.home"),
            "sourceSets" to model,
        ))))
        println("wrote " + out.get().asFile)
    }
}

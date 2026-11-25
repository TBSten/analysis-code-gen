// F-KMP を KSP2 でビルドする版（-Pacg.fixture.engine=ksp）。
// KSP2 は commonMain を metadata コンパイルで処理し、Actual・Registry は各ターゲットのコンパイルで処理する
import com.google.devtools.ksp.gradle.KspAATask
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.ksp)
}

kotlin {
    jvm()
    androidLibrary {
        namespace = "fixture.kmp"
        compileSdk = 36
        minSdk = 24
    }
    iosArm64()
    iosSimulatorArm64()
    linuxX64()
    js { nodejs() }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { nodejs() }

    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

val commonKsp = "kspCommonMainKotlinMetadata"

dependencies {
    add("kspCommonMainMetadata", acg.testProcessors.ksp)
    listOf("kspJvm", "kspAndroid", "kspIosArm64", "kspIosSimulatorArm64", "kspLinuxX64", "kspJs", "kspWasmJs").forEach {
        add(it, acg.testProcessors.ksp)
    }
}

tasks.withType<KspAATask>().configureEach {
    kspConfig.processorOptions.put("tp.processors", if (name == commonKsp) "Mapper,Chain,Defer" else "Actual,Registry")
    if (name != commonKsp) dependsOn(commonKsp)
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(commonKsp)
}

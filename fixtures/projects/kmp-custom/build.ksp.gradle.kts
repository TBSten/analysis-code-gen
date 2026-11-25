// F-KMP-CUSTOM を KSP2 でビルドする版。KSP2 は中間 SourceSet を単独で処理できないため、
// commonMain は metadata コンパイル、sharedJvmJsMain の分は jvm と js のコンパイルがそれぞれ生成する
import com.google.devtools.ksp.gradle.KspAATask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
}

kotlin {
    applyHierarchyTemplate { }
    jvm()
    js { nodejs() }
    linuxX64()

    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        }
        val sharedJvmJsMain by creating { dependsOn(commonMain.get()) }
        val nativeMain by creating { dependsOn(commonMain.get()) }
        jvmMain { dependsOn(sharedJvmJsMain) }
        jsMain { dependsOn(sharedJvmJsMain) }
        linuxX64Main { dependsOn(nativeMain) }
        jvmTest { dependsOn(commonTest.get()) }
        jsTest { dependsOn(commonTest.get()) }
        linuxX64Test { dependsOn(commonTest.get()) }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

val commonKsp = "kspCommonMainKotlinMetadata"

dependencies {
    add("kspCommonMainMetadata", acg.testProcessors.ksp)
    listOf("kspJvm", "kspJs", "kspLinuxX64").forEach { add(it, acg.testProcessors.ksp) }
}

tasks.withType<KspAATask>().configureEach {
    // commonMain の @Mapped は metadata で、sharedJvmJsMain の @Mapped は jvm・js で処理する。
    // ターゲット側で commonMain の分を二重に作らないよう、ターゲット側は sharedJvmJs のクラスだけを対象にする手段が KSP に無いので、
    // commonMain には Mapper の対象を置かない（Shared.kt 参照）
    kspConfig.processorOptions.put("tp.processors", if (name == commonKsp) "Mapper" else "Mapper,Actual")
    if (name != commonKsp) dependsOn(commonKsp)
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(commonKsp)
}

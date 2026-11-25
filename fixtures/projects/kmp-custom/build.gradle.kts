// 公開 DSL（ADR-016）で書いた F-KMP-CUSTOM。階層: commonMain ← sharedJvmJsMain ← {jvmMain, jsMain}、commonMain ← nativeMain ← linuxX64Main
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("me.tbsten.analysis.code.gen")
}

kotlin {
    applyHierarchyTemplate { } // 既定の階層を使わない
    jvm()
    js { nodejs() }
    linuxX64()

    sourceSets {
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

analysisCodeGen {
    // 中間 SourceSet（sharedJvmJsMain）に生成し、jvm と js の両方から使う（KMP-17）
    sourceSets {
        commonMain {
            processor(acg.testProcessors.native)
            arg("tp.processors", "Mapper")
        }
        named("sharedJvmJsMain") {
            processor(acg.testProcessors.native)
            arg("tp.processors", "Mapper")
        }
    }
    compilations {
        all {
            processor(acg.testProcessors.native)
            arg("tp.processors", "Actual")
        }
    }
}

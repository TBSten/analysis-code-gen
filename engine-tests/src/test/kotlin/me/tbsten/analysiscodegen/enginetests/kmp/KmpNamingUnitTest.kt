package me.tbsten.analysiscodegen.enginetests.kmp

import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.shouldBe
import me.tbsten.analysiscodegen.gradle.AnalysisCodegenNaming
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import me.tbsten.analysiscodegen.testfoundation.UnitSpec

/** F-KMP・F-KMP-CUSTOM・F-AND に現れる SourceSet と、(target, compilation) の組。 */
private val SOURCE_SETS =
    listOf(
        "main",
        "test",
        "commonMain",
        "commonTest",
        "jvmMain",
        "jvmTest",
        "iosMain",
        "appleMain",
        "nativeMain",
        "iosArm64Main",
        "iosSimulatorArm64Main",
        "linuxX64Main",
        "jsMain",
        "wasmJsMain",
        "androidMain",
        "sharedJvmJsMain",
    )
private val COMPILATIONS =
    listOf(
        "" to "main",
        "" to "test",
        "jvm" to "main",
        "jvm" to "test",
        "iosArm64" to "main",
        "iosArm64" to "test",
        "iosSimulatorArm64" to "main",
        "linuxX64" to "main",
        "js" to "main",
        "wasmJs" to "main",
        "android" to "main",
        "" to "freeDebug",
        "" to "paidRelease",
        "" to "freeDebugUnitTest",
        "" to "freeDebugAndroidTest",
    )

// 5.2 のうち KMP-23（ADR-016 の命名規則）
class KmpNamingUnitTest : UnitSpec({
    case("KMP-23", "SourceSet / Compilation 名からタスク名を作る規則が一意（analysisCodeGenIosArm64Main など、衝突しない）", PendingCase(Milestone.M1)) {
        AnalysisCodegenNaming.sourceSetTaskName("commonMain") shouldBe "analysisCodeGenCommonMain"
        AnalysisCodegenNaming.sourceSetTaskName("iosArm64Main") shouldBe "analysisCodeGenIosArm64Main"
        AnalysisCodegenNaming.sourceSetTaskName("main") shouldBe "analysisCodeGenMain"
        AnalysisCodegenNaming.compilationTaskName("jvm", "main") shouldBe "analysisCodeGenJvmMainCompilation"
        AnalysisCodegenNaming.compilationTaskName("", "main") shouldBe "analysisCodeGenMainCompilation"
        AnalysisCodegenNaming.compilationTaskName("", "freeDebug") shouldBe "analysisCodeGenFreeDebugCompilation"
        AnalysisCodegenNaming.compilationUnitName("jvm", "main") shouldBe "jvmMain"
        AnalysisCodegenNaming.compilationUnitName("iosArm64", "test") shouldBe "iosArm64Test"
        AnalysisCodegenNaming.sourceSetConfigurationName("jvmMain") shouldBe "analysisCodeGenJvmMain"
        AnalysisCodegenNaming.compilationConfigurationName("jvm", "main") shouldBe "analysisCodeGenJvmMainCompilation"

        // SourceSet 単位と Compilation 単位を合わせても、タスク名・Configuration 名・出力ディレクトリが衝突しない
        val taskNames =
            SOURCE_SETS.map(AnalysisCodegenNaming::sourceSetTaskName) +
                COMPILATIONS.map { (t, c) -> AnalysisCodegenNaming.compilationTaskName(t, c) }
        taskNames.shouldBeUnique()
        val outputDirs =
            SOURCE_SETS.map(AnalysisCodegenNaming::sourceSetOutputDir) +
                COMPILATIONS.map { (t, c) -> AnalysisCodegenNaming.compilationOutputDir(AnalysisCodegenNaming.compilationUnitName(t, c)) }
        outputDirs.shouldBeUnique()
        // jvmMain は SourceSet 名と Compilation の unit 名が同じでも、出力ディレクトリは compilations/ の下で分かれる
        (AnalysisCodegenNaming.sourceSetOutputDir("jvmMain") == AnalysisCodegenNaming.compilationOutputDir("jvmMain")) shouldBe false
    }
})

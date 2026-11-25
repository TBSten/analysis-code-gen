package me.tbsten.analysis.code.gen.e2e.api

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysis.code.gen.e2e.support.E2EEnvironment
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import me.tbsten.analysis.code.gen.testfoundation.UnitSpec

/** 利用者に公開するモジュール（ADR-012）と、ABI の dump に必ず現れるはずの宣言。 */
private val PUBLIC_MODULES =
    mapOf(
        "analysis-code-gen-processor-api" to "me/tbsten/analysis/code/gen/processor/AnalysisCodegenProcessorProvider",
        "analysis-code-gen-gradle-plugin" to "me/tbsten/analysis/code/gen/gradle/AnalysisCodegenExtension",
        "analysis-code-gen-testing" to "me/tbsten/analysis/code/gen/testing/CodegenTestResult",
        "analysis-code-gen-ksp-bridge-api" to "me/tbsten/analysis/code/gen/ksp/hybrid",
        "analysis-code-gen-ksp-bridge-gradle-plugin" to "me/tbsten/analysis/code/gen/ksp/gradle/KspBridgePlugin",
    )

// 5.3 のうち API-15。ADR-038: KGP 組み込みの ABI 検査（旧 binary-compatibility-validator）を acg.library に入れ、dump は <module>/api/<module>.api
class ApiCompatibilityUnitTest : UnitSpec({
    case("API-15", "公開 API の互換性を API dump（binary-compatibility-validator）で検査し、意図しない変更で CI が落ちる", PendingCase(Milestone.M2)) {
        val root = E2EEnvironment.repoRoot
        // 公開モジュールの convention plugin が ABI 検査を有効にし、check に含める
        val convention = root.resolve("build-logic/src/main/kotlin/acg.library.gradle.kts").readText()
        convention shouldContain "abiValidation"
        convention shouldContain "checkLegacyAbi"
        for ((module, marker) in PUBLIC_MODULES) {
            val dump = root.resolve("$module/api/$module.api")
            dump.isFile shouldBe true
            dump.readText() shouldContain marker
        }
    }
})

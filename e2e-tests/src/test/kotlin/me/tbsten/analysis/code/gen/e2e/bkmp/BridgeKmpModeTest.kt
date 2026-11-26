package me.tbsten.analysis.code.gen.e2e.bkmp

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import me.tbsten.analysis.code.gen.e2e.support.Acg45Repo
import me.tbsten.analysis.code.gen.e2e.support.mappedClass
import me.tbsten.analysis.code.gen.e2e.support.probeValue
import me.tbsten.analysis.code.gen.e2e.support.readProbeLines
import me.tbsten.analysis.code.gen.testfoundation.DiffSpec
import me.tbsten.analysis.code.gen.testfoundation.E2ESpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import me.tbsten.analysis.code.gen.testing.gradle.GradleFixture
import me.tbsten.analysis.code.gen.testing.gradle.gradleFixture
import me.tbsten.analysis.code.gen.testprocessors.TpAnnotationsSource
import java.io.File
import java.nio.file.Files

// 5.17 Bridge の KMP 実行モード（BKMP、ADR-017）。クラウドの Agent 環境でも動くよう、ターゲットは jvm と js にする（同じ種類のターゲットを 2 つ宣言することは KGP が許さない）。
// commonMain は両方の Compilation に入る。Apple / Native を含む F-KMP 版は CI（fixtures.yml）の範囲

private val tp = Acg45Repo.coordinate("test-processors-ksp")

/** KMP（jvm + js）のフィクスチャ。[body] はビルドスクリプトの plugins の後ろ。 */
private fun kmpProject(
    plugins: String,
    body: String,
    properties: Map<String, String> = emptyMap(),
): GradleFixture =
    gradleFixture {
        val repo = "maven { url = uri(\"${Acg45Repo.fixtureRepo.toURI()}\") }"
        extraPluginRepositories += repo
        extraRepositories += repo
        gradleProperties += properties
        buildScript =
            """
            plugins {
                kotlin("multiplatform") version "{{kotlinVersion}}"
                $plugins
            }

            kotlin {
                jvm()
                js { nodejs() }
            }

            $body
            """.trimIndent()
        file("src/commonMain/kotlin/me/tbsten/analysis/code/gen/tp/${TpAnnotationsSource.FILE_NAME}", TpAnnotationsSource.content)
        kotlin("src/commonMain/kotlin/com/example/Common.kt", mappedClass("Common"))
        kotlin("src/jvmMain/kotlin/com/example/OnJvm.kt", mappedClass("OnJvm"))
        kotlin("src/jsMain/kotlin/com/example/OnJs.kt", mappedClass("OnJs"))
    }

private val ksp2Plugin = "id(\"com.google.devtools.ksp\") version \"${Acg45Repo.kspVersion}\""
private val bridgePlugin = "id(\"me.tbsten.analysis.code.gen.ksp-bridge\") version \"${Acg45Repo.acgVersion}\""

/** [root] 以下の生成 Kotlin を「Compilation（または SourceSet）名 / ファイル名 → 中身」で集める。 */
private fun collect(
    root: File,
    unitOf: (File) -> String,
): Map<String, String> =
    if (!root.isDirectory) {
        emptyMap()
    } else {
        root
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .associate { unitOf(it) + "/" + it.name to it.readText() }
            .toSortedMap()
    }

/** KSP2 の生成物: build/generated/ksp/<target>/<compilation>Main/kotlin/... → `<target>Main/<File>`。 */
private fun GradleFixture.ksp2Outputs(): Map<String, String> {
    val root = projectDir.resolve("build/generated/ksp")
    return collect(root) { it.relativeTo(root).invariantSeparatorsPath.split('/')[1] }
}

/** analysisCodeGen の Compilation 単位の生成物: build/generated/analysisCodeGen/compilations/<Name>/kotlin/... → `<Name>/<File>`。 */
private fun GradleFixture.compilationOutputs(): Map<String, String> {
    val root = projectDir.resolve("build/generated/analysisCodeGen/compilations")
    return collect(root) { it.relativeTo(root).invariantSeparatorsPath.substringBefore('/') }
}

private fun GradleFixture.sourceSetOutputs(): Map<String, String> {
    val root = projectDir.resolve("build/generated/analysisCodeGen")
    return collect(root) { it.relativeTo(root).invariantSeparatorsPath.substringBefore('/') }.filterKeys { !it.startsWith("compilations/") }
}

class BridgeKmpModeDiffTest : DiffSpec({
    case("BKMP-01", "Compatible モードでは KSP2 と同じく Compilation 単位で実行し、生成物が KSP2 と一致する", pending = PendingCase(Milestone.M3)) {
        val ksp2 =
            kmpProject(
                ksp2Plugin,
                """
                ksp { arg("tp.processors", "Mapper,Registry") }
                dependencies {
                    add("kspJvm", "$tp")
                    add("kspJs", "$tp")
                }
                """.trimIndent(),
            )
        ksp2.build("kspKotlinJvm", "kspKotlinJs")

        val bridge =
            kmpProject(
                bridgePlugin,
                """
                analysisCodeGen {
                    arg("tp.processors", "Mapper,Registry")
                    kspBridge { processingMode = me.tbsten.analysis.code.gen.ksp.gradle.KspProcessingMode.Compatible }
                    compilations {
                        named("jvmMain") { processor("$tp") }
                        named("jsMain") { processor("$tp") }
                    }
                }
                """.trimIndent(),
            )
        bridge.build("analysisCodeGenJvmMainCompilation", "analysisCodeGenJsMainCompilation")

        val expected = ksp2.ksp2Outputs()
        expected.keys shouldContain "jvmMain/CommonMapper.kt"
        bridge.compilationOutputs() shouldBe expected
    }

    case("BKMP-04", "KSP の ksp.experimental.metadata.own.sources.only を使う構成と同じ結果が得られる", pending = PendingCase(Milestone.M3)) {
        // KSP2: commonMain の metadata コンパイルに Processor を付け、own sources only で commonMain のソースだけを処理する。
        // Bridge: SourceSet モード + commonMain の processingScope = Own（ADR-016・017）
        val ksp2 =
            kmpProject(
                ksp2Plugin,
                """
                ksp { arg("tp.processors", "Mapper") }
                dependencies { add("kspCommonMainMetadata", "$tp") }
                """.trimIndent(),
                properties = mapOf("ksp.experimental.metadata.own.sources.only" to "true"),
            )
        ksp2.build("kspCommonMainKotlinMetadata")
        val bridge =
            kmpProject(
                bridgePlugin,
                """
                analysisCodeGen {
                    arg("tp.processors", "Mapper")
                    kspBridge { processingMode = me.tbsten.analysis.code.gen.ksp.gradle.KspProcessingMode.SourceSet }
                    sourceSets { commonMain { processor("$tp") } }
                }
                """.trimIndent(),
            )
        bridge.build("analysisCodeGenCommonMainSourceSet")
        val expected = ksp2.ksp2Outputs().mapKeys { it.key.substringAfter('/') }
        expected.keys shouldBe setOf("CommonMapper.kt")
        bridge.sourceSetOutputs().mapKeys { it.key.substringAfter('/') } shouldBe expected
    }
})

class BridgeKmpModeE2ETest : E2ESpec({
    case("BKMP-02", "SourceSet モードでは commonMain の生成が 1 回だけで、下流で再生成されない", pending = PendingCase(Milestone.M3)) {
        val probe = Files.createTempFile("acg-probe", ".jsonl").toFile().apply { delete() }
        val fixture =
            kmpProject(
                bridgePlugin,
                """
                analysisCodeGen {
                    arg("tp.processors", "Mapper,Probe")
                    arg("tp.probe.out", "${probe.absolutePath.replace("\\", "/")}")
                    kspBridge { processingMode = me.tbsten.analysis.code.gen.ksp.gradle.KspProcessingMode.SourceSet }
                    sourceSets { all { processor("$tp") } }
                }
                """.trimIndent(),
            )
        fixture.build("compileKotlinJvm", "compileKotlinJs")
        val outputs = fixture.sourceSetOutputs()
        outputs.keys.filter { it.endsWith("/CommonMapper.kt") } shouldBe listOf("commonMain/CommonMapper.kt")
        outputs.keys shouldContain "jvmMain/OnJvmMapper.kt"
        outputs.keys shouldContain "jsMain/OnJsMapper.kt"
        // Common.kt を処理した Processor は 1 つ（commonMain）だけ
        val processedCommon =
            readProbeLines(probe).filter {
                probeValue(it, "event") == "process" &&
                    it.contains("Common.kt") &&
                    it.contains("\"newFiles\":[\"")
            }
        processedCommon.shouldNotBeEmpty()
        processedCommon.map { probeValue(it, "instance") }.distinct().size shouldBe 1
    }

    case("BKMP-03", "KSP のターゲット別指定（kspJvm、kspIosArm64 相当）からの移行先があり、同じ結果になる", pending = PendingCase(Milestone.M3)) {
        // ADR-017: kspJvm(...) の移行先は analysisCodeGenJvmMainCompilation(...)
        val ksp2 =
            kmpProject(
                ksp2Plugin,
                """
                ksp { arg("tp.processors", "Mapper,Registry") }
                dependencies { add("kspJvm", "$tp") }
                """.trimIndent(),
            )
        ksp2.build("kspKotlinJvm")
        val bridge =
            kmpProject(
                bridgePlugin,
                """
                analysisCodeGen { arg("tp.processors", "Mapper,Registry") }
                dependencies { add("analysisCodeGenJvmMainCompilation", "$tp") }
                """.trimIndent(),
            )
        bridge.build("analysisCodeGenJvmMainCompilation")
        val expected = ksp2.ksp2Outputs()
        expected.keys.all { it.startsWith("jvmMain/") } shouldBe true
        bridge.compilationOutputs() shouldBe expected
    }
})

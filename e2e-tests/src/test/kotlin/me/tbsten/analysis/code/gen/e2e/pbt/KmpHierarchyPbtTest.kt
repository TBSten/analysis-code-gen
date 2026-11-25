package me.tbsten.analysis.code.gen.e2e.pbt

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import me.tbsten.analysis.code.gen.e2e.support.repoRoot
import me.tbsten.analysis.code.gen.pbt.kmp.KmpHierarchy
import me.tbsten.analysis.code.gen.pbt.kmp.KmpTarget
import me.tbsten.analysis.code.gen.pbt.kmp.kmpHierarchy
import me.tbsten.analysis.code.gen.pbt.run.acgCheckAll
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PbtSpec
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import me.tbsten.analysis.code.gen.testing.gradle.GradleFixture
import me.tbsten.analysis.code.gen.testing.gradle.gradleFixture
import me.tbsten.analysis.code.gen.testprocessors.TpAnnotationsSource

// PBT-04: ランダムな KMP 階層（SourceSet の木・ターゲットの組み合わせ、:pbt の kmpHierarchy）で、
// 共通の生成が 1 回だけ・可視範囲が正しい・全ターゲットでコンパイルできる
// 各 SourceSet に @Mapped のクラスを 1 つ置き、TP-Mapper（Native 版）を全 SourceSet に付ける。各ターゲットの葉には、
// 祖先の SourceSet すべての生成物（<SourceSet>ModelMapper）を使うコードを置く。コンパイルが通れば「見えるべきものが見える」

private val isMac = System.getProperty("os.name").lowercase().contains("mac")

/** Gradle の DSL でのターゲットの宣言。Android は AGP が要るので外す（F-KMP と CI の fixtures.yml で見る）。 */
private fun KmpTarget.dsl(): String? =
    when (this) {
        KmpTarget.JVM -> "jvm()"
        KmpTarget.ANDROID -> null
        KmpTarget.JS -> "js { nodejs() }"
        KmpTarget.WASM_JS -> "@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class) wasmJs { nodejs() }"
        KmpTarget.IOS_ARM64 -> "iosArm64()"
        KmpTarget.IOS_SIMULATOR_ARM64 -> "iosSimulatorArm64()"
        KmpTarget.MACOS_ARM64 -> "macosArm64()"
        KmpTarget.LINUX_X64 -> "linuxX64()"
        KmpTarget.MINGW_X64 -> "mingwX64()"
    }

private fun usable(h: KmpHierarchy): Boolean = h.problems().isEmpty() && h.targets.all { it.dsl() != null && (isMac || !it.needsMac) }

private fun String.model() = replaceFirstChar { it.uppercase() } + "Model"

private fun acgVersion(): String =
    repoRoot
        .resolve("gradle.properties")
        .readLines()
        .first { it.startsWith("acg.version=") }
        .substringAfter("=")
        .trim()

/** 階層から KMP のフィクスチャを作る。 */
private fun kmpFixture(h: KmpHierarchy): GradleFixture {
    val version = acgVersion()
    val repo = repoRoot.resolve("build/fixture-repo").invariantSeparatorsPath
    return gradleFixture {
        extraPluginRepositories += "maven(\"$repo\")"
        extraRepositories += "maven(\"$repo\")"
        // 既定の階層テンプレートを使わず、階層の木を dependsOn でそのまま書く
        gradleProperties["kotlin.mpp.applyDefaultHierarchyTemplate"] = "false"
        val created = h.sourceSets.filter { it != KmpHierarchy.COMMON && h.targets.none { t -> t.sourceSet == it } }
        buildScript =
            """
            |plugins {
            |    kotlin("multiplatform") version "{{kotlinVersion}}"
            |    id("me.tbsten.analysis.code.gen") version "$version"
            |}
            |
            |kotlin {
            |${h.targets.joinToString("\n") { "    ${it.dsl()}" }}
            |    sourceSets {
            |${created.joinToString("\n") { "        val $it by creating" }}
            |${h.parents.filterValues { it != null }.entries.joinToString(
                "\n",
            ) { (s, p) -> "        getByName(\"$s\").dependsOn(getByName(\"$p\"))" }}
            |    }
            |}
            |
            |analysisCodeGen {
            |    arg("tp.processors", "Mapper")
            |    sourceSets.configureEach { processor("me.tbsten.analysis.code.gen:test-processors-native:$version") }
            |}
            """.trimMargin()
        kotlin("src/commonMain/kotlin/me/tbsten/analysis/code/gen/tp/${TpAnnotationsSource.FILE_NAME}", TpAnnotationsSource.content)
        for (s in h.sourceSets) {
            kotlin(
                "src/$s/kotlin/kmp/${s.model()}.kt",
                "package kmp\n\n@me.tbsten.analysis.code.gen.tp.Mapped class ${s.model()}(val v: Int)",
            )
        }
        for (t in h.targets) {
            val uses = h.visibleFrom(t.sourceSet).joinToString(",\n") { "    ${it.model()}Mapper.toMap(${it.model()}(1))" }
            kotlin(
                "src/${t.sourceSet}/kotlin/kmp/Use${t.sourceSet.model()}.kt",
                "package kmp\n\nfun use${t.sourceSet.model()}() = listOf(\n$uses,\n)",
            )
        }
    }
}

class KmpHierarchyPbtTest : PbtSpec({
    case(
        "PBT-04",
        "ランダムな KMP 階層（SourceSet の木・ターゲットの組み合わせ）で、共通の生成が 1 回だけ・可視範囲が正しい・全ターゲットでコンパイルできる",
        pending = PendingCase(Milestone.M2),
    ) {
        val arb = Arb.kmpHierarchy().filter(::usable)
        acgCheckAll("PBT-04 KMP 階層によらず共通の生成が 1 回だけで全ターゲットがコンパイルできる", arb, prIterations = 3) { h ->
            val fixture = kmpFixture(h)
            // 全ターゲットのコンパイル（メタデータを含む）。祖先の生成物が見えなければ Use*.kt がコンパイルできない
            val result = fixture.build("assemble", "--continue")
            withClue(h) {
                result.output.contains("BUILD SUCCESSFUL") shouldBe true
                val generatedRoot = fixture.projectDir.resolve("build/generated/analysisCodeGen")
                // 各 SourceSet の Mapper は、その SourceSet の実行単位でだけ 1 回作られる（子の SourceSet や Compilation で作り直さない）
                val mappers =
                    generatedRoot
                        .walkTopDown()
                        .filter { it.isFile && it.name.endsWith("ModelMapper.kt") }
                        .groupBy({ it.name }, { it.relativeTo(generatedRoot).invariantSeparatorsPath.substringBefore('/') })
                for (s in h.sourceSets) {
                    mappers["${s.model()}Mapper.kt"] shouldBe listOf(s)
                }
                // 余分な生成物が無い
                (mappers.keys - h.sourceSets.map { "${it.model()}Mapper.kt" }.toSet()).shouldBeEmpty()
            }
        }
    }
})

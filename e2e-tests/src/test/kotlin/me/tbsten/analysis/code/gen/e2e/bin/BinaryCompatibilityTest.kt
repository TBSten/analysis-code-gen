package me.tbsten.analysis.code.gen.e2e.bin

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysis.code.gen.e2e.support.Acg45Engine
import me.tbsten.analysis.code.gen.e2e.support.Acg45Repo
import me.tbsten.analysis.code.gen.e2e.support.acg45Project
import me.tbsten.analysis.code.gen.e2e.support.generatedNames
import me.tbsten.analysis.code.gen.e2e.support.mappedClass
import me.tbsten.analysis.code.gen.testfoundation.E2ESpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import me.tbsten.analysis.code.gen.testing.TestEngine
import me.tbsten.analysis.code.gen.testing.gradle.GradleFixture
import me.tbsten.analysis.code.gen.testing.gradle.gradleFixture
import me.tbsten.analysis.code.gen.testing.runCodegenTest
import org.gradle.testkit.runner.TaskOutcome

// 5.18 バイナリ互換（BIN）。Maven Central の JAR をそのまま Bridge に渡す。Moshi は ADR-019 の対象 Processor

/** Moshi の codegen（KSP）を Bridge で動かすフィクスチャ。 */
private fun moshiProject(codegenVersion: String): GradleFixture =
    gradleFixture {
        val repo = "maven { url = uri(\"${Acg45Repo.fixtureRepo.toURI()}\") }"
        extraPluginRepositories += repo
        extraRepositories += repo
        buildScript =
            """
            plugins {
                kotlin("jvm") version "{{kotlinVersion}}"
                id("me.tbsten.analysis.code.gen.ksp-bridge") version "${Acg45Repo.acgVersion}"
            }

            analysisCodeGen {
                sourceSets { main { processor("com.squareup.moshi:moshi-kotlin-codegen:$codegenVersion") } }
            }

            dependencies {
                implementation("com.squareup.moshi:moshi:1.15.2")
            }
            """.trimIndent()
        kotlin(
            "src/main/kotlin/com/example/User.kt",
            """
            package com.example

            import com.squareup.moshi.JsonClass

            @JsonClass(generateAdapter = true)
            data class User(val id: Int, val name: String, val tags: List<String> = emptyList())
            """,
        )
        kotlin(
            "src/main/kotlin/com/example/Main.kt",
            """
            package com.example

            import com.squareup.moshi.Moshi

            fun roundTrip(): String {
                val adapter = UserJsonAdapter(Moshi.Builder().build())
                return adapter.toJson(adapter.fromJson("{\"id\":1,\"name\":\"a\"}")!!)
            }
            """,
        )
    }

private fun GradleFixture.shouldGenerateMoshiAdapter() {
    val result = build("compileKotlin")
    result.task(":compileKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
    projectDir.resolve("build/generated/analysisCodeGen/main/kotlin/com/example/UserJsonAdapter.kt").readText() shouldContain
        "class UserJsonAdapter"
}

/** KSP の内部実装クラス（symbol-processing の impl パッケージ）に触れる Processor。Bridge では存在しない。 */
private const val KSP_INTERNAL_CLASS = "com.google.devtools.ksp.impl.KotlinSymbolProcessing"

private val internalUser =
    SymbolProcessorProvider { _ ->
        object : SymbolProcessor {
            override fun process(resolver: Resolver): List<KSAnnotated> {
                // 実在の Processor では、Resolver を内部の実装クラスにキャストするなどの形で起きる
                Class.forName(KSP_INTERNAL_CLASS)
                return emptyList()
            }
        }
    }

class BinaryCompatibilityTest : E2ESpec({
    case("BIN-01", "Maven Central で公開済みの Processor JAR を、再ビルドせずに Bridge で実行できる", pending = PendingCase(Milestone.M1)) {
        moshiProject("1.15.2").shouldGenerateMoshiAdapter()
    }

    case("BIN-02", "対応する KSP API バージョンの最古と最新でビルドされた Processor がどちらも動く", pending = PendingCase(Milestone.M3)) {
        // ADR-035: 対応する KSP API の最古は KSP 1.7 系、最新は version catalog の ksp。
        // 最古の代表は moshi-kotlin-codegen 1.14.0（Kotlin 1.7 / KSP 1.7 でビルド）、最新の代表は TP の KSP 版（catalog の ksp でビルド）
        moshiProject("1.14.0").shouldGenerateMoshiAdapter()
        val latest =
            acg45Project(Acg45Engine.Bridge, processors = "Mapper") {
                kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A"))
            }
        latest.build("compileKotlin").task(":compileKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
        latest.generatedNames(Acg45Engine.Bridge) shouldContain "AMapper.kt"
    }

    case("BIN-03", "KSP の内部実装クラスに依存する Processor では、原因が分かる診断が出る", pending = PendingCase(Milestone.M3)) {
        // ADR-035: 診断には、見つからなかった KSP の内部クラス名と「KSP の内部実装（公開 API 外）に依存している」旨が出る
        val result =
            runCodegenTest(TestEngine.AnalysisCodegen) {
                kotlin("A.kt", "class A")
                ksp(internalUser)
            }
        result.isSuccess shouldBe false
        val message = result.errors.joinToString("\n") { it.message }
        message shouldContain KSP_INTERNAL_CLASS
        message shouldContain "KSP の内部実装"
    }
})

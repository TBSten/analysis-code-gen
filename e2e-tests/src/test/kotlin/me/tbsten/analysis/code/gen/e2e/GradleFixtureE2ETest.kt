package me.tbsten.analysis.code.gen.e2e

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysis.code.gen.testing.gradle.gradleFixture
import org.gradle.testkit.runner.TaskOutcome

// TST-06 の土台（ケース本体は ACG-44〜46 の保留テスト）: TestKit のフィクスチャを実際にビルドし、Kotlin の版の切り替え（-Pacg.kotlin、ADR-011）が効くことを見る
class GradleFixtureE2ETest : FreeSpec({
    "kotlin(\"jvm\") のフィクスチャを指定の Kotlin でビルドできる" {
        val fixture =
            gradleFixture {
                kotlinJvm(
                    """
                // coreLibrariesVersion の既定値は KGP の版
                val kgpVersion = kotlin.coreLibrariesVersion
                tasks.register("printKotlinVersion") { doLast { println("KGP=" + kgpVersion) } }
                """,
                )
                kotlin("src/main/kotlin/com/example/Foo.kt", "package com.example\n\nclass Foo { fun hi() = \"hi\" }")
            }

        val result = fixture.build("compileKotlin", "printKotlinVersion")

        result.task(":compileKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
        result.output shouldContain "KGP=${fixture.kotlinVersion}"
        fixture.projectDir.resolve("build/classes/kotlin/main/com/example/Foo.class").isFile shouldBe true
    }

    "コンパイルエラーのフィクスチャは失敗として取れる" {
        val fixture =
            gradleFixture {
                kotlinJvm()
                kotlin("src/main/kotlin/Broken.kt", "val x: Int = \"s\"")
            }

        val result = fixture.buildAndFail("compileKotlin")

        result.task(":compileKotlin")?.outcome shouldBe TaskOutcome.FAILED
    }
})

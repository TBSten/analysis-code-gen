package me.tbsten.analysiscodegen.testing.gradle

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

// 設定ファイルの書き出しだけを見る（実際のビルドは e2e-tests の GradleFixtureE2ETest）
class GradleFixtureTest : FreeSpec({
    "Kotlin の版は明示した値が build.gradle.kts に入る" {
        val fixture =
            gradleFixture {
                kotlinVersion = "2.4.0"
                kotlinJvm()
            }
        fixture.writeSettings()

        fixture.projectDir.resolve("build.gradle.kts").readText() shouldContain "kotlin(\"jvm\") version \"2.4.0\""
    }

    "既定の Kotlin の版は acg.kotlin、無ければ testing のコンパイラの版" {
        val expected = System.getProperty("acg.kotlin") ?: org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION
        GradleFixture.defaultKotlinVersion() shouldBe expected
    }

    "acg.mavenMirror があれば最初のリポジトリになる" {
        val before = System.getProperty("acg.mavenMirror")
        try {
            System.setProperty("acg.mavenMirror", "https://mirror.example/maven2/")
            val fixture = gradleFixture { kotlinJvm() }
            fixture.writeSettings()
            val settings = fixture.projectDir.resolve("settings.gradle.kts").readText()
            settings.indexOf("maven(\"https://mirror.example/maven2/\")") shouldBe
                settings.indexOf("repositories {") + "repositories {\n        ".length
        } finally {
            if (before == null) System.clearProperty("acg.mavenMirror") else System.setProperty("acg.mavenMirror", before)
        }
    }

    "acg.mavenMirror が無ければミラーを書かない" {
        if (System.getProperty("acg.mavenMirror") == null) {
            val fixture = gradleFixture { kotlinJvm() }
            fixture.writeSettings()
            fixture.projectDir.resolve("settings.gradle.kts").readText() shouldNotContain "maven(\""
        }
    }

    "include したサブプロジェクトが settings に入る" {
        val fixture =
            gradleFixture {
                kotlinJvm()
                includes += ":lib"
            }
        fixture.writeSettings()
        fixture.projectDir.resolve("settings.gradle.kts").readText() shouldContain "include(\":lib\")"
    }
})

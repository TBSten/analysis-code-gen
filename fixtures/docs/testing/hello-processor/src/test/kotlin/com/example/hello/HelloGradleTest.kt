package com.example.hello

import me.tbsten.analysiscodegen.testing.gradle.gradleFixture
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

class HelloGradleTest {
    private val processorJar = System.getProperty("hello.processorJar")

    @Test
    fun `Gradle のビルドで生成とコンパイルが通る`() {
        val fixture = gradleFixture {
            buildScript = """
                plugins {
                    kotlin("jvm") version "{{kotlinVersion}}"
                    id("me.tbsten.analysiscodegen") version "0.1.0"
                }
                dependencies { analysisCodeGen(files("$processorJar")) }
            """
            kotlin("src/main/kotlin/com/example/Greeter.kt", "package com.example\n\n@Hello class Greeter")
            kotlin("src/main/kotlin/com/example/Hello.kt", "package com.example\n\nannotation class Hello")
        }
        val result = fixture.build("compileKotlin")
        assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlin")?.outcome)
    }
}

package me.tbsten.analysis.code.gen.e2e.support

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files

// support の補助テスト（ケース ID なし）
class MiniJsonTest : FreeSpec({
    "ProbeEvent の 1 行を読める" {
        val f = Files.createTempFile("probe", ".jsonl").toFile()
        f.writeText(
            """{"event":"process","frontend":"native","instance":"a\"b","round":2,""" +
                """"options":{"k":"v"},"allFiles":["A.kt","B.kt"],"extra":{}}""" + "\n",
        )
        val r = readProbe(f).single()
        r.event shouldBe "process"
        r.instance shouldBe "a\"b"
        r.round shouldBe 2
        r.options shouldBe mapOf("k" to "v")
        r.allFiles shouldBe listOf("A.kt", "B.kt")
        r.sourceSet shouldBe null
    }

    "フィクスチャを一時ディレクトリへ複製でき、元は書き換えない" {
        val p = fixtureProject(Fixture.JVM) { edit("build.gradle.kts") { it + "\n// changed" } }
        p.file("settings.gradle.kts").isFile shouldBe true
        p.dir.resolve("../build-support/settings.gradle.kts").isFile shouldBe true
        E2EEnvironment.repoRoot
            .resolve("fixtures/projects/jvm/build.gradle.kts")
            .readText()
            .endsWith("// changed") shouldBe false
    }
})

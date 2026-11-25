package me.tbsten.analysis.code.gen.e2e.out

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysis.code.gen.e2e.support.Acg45Engine
import me.tbsten.analysis.code.gen.e2e.support.acg45Project
import me.tbsten.analysis.code.gen.e2e.support.codegenTask
import me.tbsten.analysis.code.gen.e2e.support.generatedFiles
import me.tbsten.analysis.code.gen.e2e.support.generatedNames
import me.tbsten.analysis.code.gen.e2e.support.generatedRoot
import me.tbsten.analysis.code.gen.e2e.support.mappedClass
import me.tbsten.analysis.code.gen.testfoundation.E2ESpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase

// 5.11 不要な生成物の削除（OUT）。出力ディレクトリはタスクが所有する（ADR-014）
private val native = Acg45Engine.Native
private val task = codegenTask(native)

class StaleOutputE2ETest : E2ESpec({
    case("OUT-01", "ソースを消すと、それから作った生成物が消える", pending = PendingCase(Milestone.M2)) {
        val fixture =
            acg45Project(processors = "Mapper") {
                kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A"))
                kotlin("src/main/kotlin/com/example/B.kt", mappedClass("B"))
            }
        fixture.build(task)
        fixture.generatedNames(native) shouldContain "BMapper.kt"

        fixture.projectDir.resolve("src/main/kotlin/com/example/B.kt").delete() shouldBe true
        fixture.build(task)
        fixture.generatedNames(native) shouldNotContain "BMapper.kt"
        fixture.generatedNames(native) shouldContain "AMapper.kt"
        // 生成物が残っていると、消したクラスを参照するコードがコンパイルに残る。コンパイルも通ること
        fixture.build("compileKotlin")
    }

    case("OUT-02", "Processor を依存から外すと、その Processor の過去の生成物が消える", pending = PendingCase(Milestone.M2)) {
        val fixture = acg45Project(processors = "Mapper") { kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A")) }
        fixture.build(task)
        fixture.generatedNames(native) shouldContain "AMapper.kt"

        // processor(...) を消す。Processor の無い実行単位はタスクを登録しない（ADR-016）が、過去の生成物は残さない
        fixture.buildScript = fixture.buildScript.replace(Regex("""sourceSets \{ main \{ processor\("[^"]+"\) } }"""), "")
        fixture.build("compileKotlin")
        fixture.generatedFiles(native) shouldBe emptyMap()
    }

    case("OUT-03", "出力パスが変わった（パッケージ変更）とき、古いファイルが残らない", pending = PendingCase(Milestone.M2)) {
        val fixture = acg45Project(processors = "Mapper") { kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A")) }
        fixture.build(task)
        fixture.generatedFiles(native).keys.single { it.endsWith("AMapper.kt") } shouldContain "com/example/"

        // 同じファイルのままパッケージだけ変える
        fixture.kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A", pkg = "com.other"))
        fixture.build(task)
        val mappers = fixture.generatedFiles(native).keys.filter { it.endsWith("AMapper.kt") }
        mappers.size shouldBe 1
        mappers.single() shouldContain "com/other/"
    }

    case("OUT-04", "依存の変化で出力集合が小さくなったら、余った出力が消える", pending = PendingCase(Milestone.M2)) {
        // TP-Defer は target が解決できたときだけ <Class>Resolved を作る。依存（ライブラリ）から target が消えると出力も消える
        val coroutines = "dependencies { implementation(\"org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2\") }"
        val fixture =
            acg45Project(processors = "Defer", extraBuild = coroutines) {
                kotlin(
                    "src/main/kotlin/com/example/Waits.kt",
                    """
                    package com.example

                    import me.tbsten.analysis.code.gen.tp.DeferUntil

                    @DeferUntil("kotlinx.coroutines.Job")
                    class Waits
                    """,
                )
            }
        fixture.build(task)
        fixture.generatedNames(native) shouldContain "WaitsResolved.kt"

        fixture.buildScript = fixture.buildScript.replace(coroutines, "")
        // target が解決できず Deferred が残るので、警告に落として生成側の結果だけを見る
        fixture.buildScript =
            fixture.buildScript.replace(
                "analysisCodeGen {",
                "analysisCodeGen {\n    unresolvedDeferred = me.tbsten.analysis.code.gen.gradle.DeferredSeverity.Warning",
            )
        fixture.build(task)
        fixture.generatedNames(native) shouldNotContain "WaitsResolved.kt"
    }

    case("OUT-05", "生成ディレクトリに手で置かれたファイルを ADR どおりに扱う", pending = PendingCase(Milestone.M2)) {
        // ADR-014: 出力ディレクトリはタスクが所有する。マニフェストに無いファイルは Build Commit で消し、warning を出す
        val fixture = acg45Project(processors = "Mapper") { kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A")) }
        fixture.build(task)
        val stray = fixture.generatedRoot(native).resolve("kotlin/com/example/HandWritten.kt")
        stray.parentFile.mkdirs()
        stray.writeText("package com.example\n\nclass HandWritten")
        fixture.kotlin("src/main/kotlin/com/example/B.kt", mappedClass("B"))

        val result = fixture.build(task)
        stray.exists() shouldBe false
        result.output shouldContain "HandWritten.kt"
        fixture.generatedNames(native) shouldContain "BMapper.kt"
    }
})

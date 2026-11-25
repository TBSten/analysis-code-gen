package me.tbsten.analysis.code.gen.e2e.binc

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysis.code.gen.e2e.support.Acg45Engine
import me.tbsten.analysis.code.gen.e2e.support.acg45Project
import me.tbsten.analysis.code.gen.e2e.support.codegenTask
import me.tbsten.analysis.code.gen.e2e.support.mappedClass
import me.tbsten.analysis.code.gen.e2e.support.probeArray
import me.tbsten.analysis.code.gen.e2e.support.probeValue
import me.tbsten.analysis.code.gen.e2e.support.readProbeLines
import me.tbsten.analysis.code.gen.testfoundation.DiffSpec
import me.tbsten.analysis.code.gen.testfoundation.E2ESpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import me.tbsten.analysis.code.gen.testing.TestEngine
import me.tbsten.analysis.code.gen.testing.gradle.GradleFixture
import me.tbsten.analysis.code.gen.testing.runCodegenScenario
import java.io.File
import java.nio.file.Files

// 5.15 Bridge の増分互換（BINC）。KSP 版の TP-Probe の newFiles（= getNewFiles）が、そのビルドで dirty とされたファイル

private fun probeFile(): File = Files.createTempFile("acg-probe", ".jsonl").toFile().apply { delete() }

/** 最後の実行のラウンド 1 の newFiles（ファイル名、TpAnnotations.kt を除く）。 */
private fun lastDirty(probe: File): Set<String> {
    val lines = readProbeLines(probe)
    val lastCreate = lines.indexOfLast { probeValue(it, "event") == "create" }
    return lines
        .drop(lastCreate + 1)
        .first { probeValue(it, "event") == "process" && probeValue(it, "round") == "1" }
        .let { probeArray(it, "newFiles") }
        .map { it.substringAfterLast('/') }
        .filterNot { it == "TpAnnotations.kt" }
        .toSet()
}

/** 型解決を通じた暗黙の依存（設計文書 11.7）: A の出力は A.kt だけを明示し、中身は上位型 B（B.kt）のプロパティで決まる。 */
private val supersProvider =
    SymbolProcessorProvider { env ->
        object : SymbolProcessor {
            override fun process(resolver: Resolver): List<KSAnnotated> {
                resolver.getSymbolsWithAnnotation("com.example.Describe").filterIsInstance<KSClassDeclaration>().forEach { a ->
                    val b =
                        a.superTypes
                            .first()
                            .resolve()
                            .declaration as KSClassDeclaration
                    val props =
                        b
                            .getAllProperties()
                            .map { it.simpleName.asString() }
                            .toList()
                            .sorted()
                    env.codeGenerator
                        .createNewFile(
                            Dependencies(false, a.containingFile!!),
                            "com.example",
                            "${a.simpleName.asString()}Supers",
                        ).use {
                            it.write(
                                "package com.example\n\nval ${a.simpleName.asString().lowercase()}SuperProps = \"${props.joinToString(
                                    ",",
                                )}\"\n".toByteArray(),
                            )
                        }
                }
                return emptyList()
            }
        }
    }

private fun binc02(engine: TestEngine) {
    val steps =
        runCodegenScenario(engine) {
            initial {
                kotlin("com/example/Describe.kt", "package com.example\n\nannotation class Describe")
                kotlin("com/example/A.kt", "package com.example\n\n@Describe\nclass A : B()")
                kotlin("com/example/B.kt", "package com.example\n\nopen class B { val first = 1 }")
                ksp(supersProvider)
            }
            step("B にプロパティを足す（A.kt は変えない）") {
                kotlin("com/example/B.kt", "package com.example\n\nopen class B { val first = 1\n val second = 2 }")
            }
        }
    steps.forEach { it.result.isSuccess shouldBe true }
    steps[0].result.generatedFile("ASupers.kt") shouldContain "\"first\""
    steps[1].result.generatedFile("ASupers.kt") shouldContain "\"first,second\""
}

class BridgeIncrementalE2ETest : E2ESpec({
    case("BINC-01", "KSP の Dependencies(isolating) の契約どおりに再処理される", pending = PendingCase(Milestone.M2)) {
        // TP-Mapper（KSP 版）は Dependencies(false, そのクラスのファイル) で書く。A.kt を変えたら A.kt だけが dirty
        val probe = probeFile()
        val fixture =
            acg45Project(Acg45Engine.Bridge, processors = "Mapper,Probe", probeOut = probe) {
                kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A"))
                kotlin("src/main/kotlin/com/example/B.kt", mappedClass("B"))
            }
        val task = codegenTask(Acg45Engine.Bridge)
        fixture.build(task)
        fixture.kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A", props = "val id: Int, val name: String"))
        fixture.build(task)
        lastDirty(probe) shouldBe setOf("A.kt")
    }

    case("BINC-02", "型解決を通じた暗黙の依存（設計文書 11.7 の superTypes の例）で、B.kt を変えると再生成される", pending = PendingCase(Milestone.M3)) {
        binc02(TestEngine.AnalysisCodegen)
    }
    "[KSP2 で期待を確認] BINC-02" { binc02(TestEngine.Ksp2) }
})

/** KSP2 と Bridge で同じ変更を当て、dirty とされたファイルを返す。 */
private fun dirtyAfterChange(
    engine: Acg45Engine,
    change: GradleFixture.() -> Unit,
): Set<String> {
    val probe = probeFile()
    val fixture =
        acg45Project(engine, processors = "Mapper,Registry,Probe", probeOut = probe) {
            kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A"))
            kotlin("src/main/kotlin/com/example/B.kt", mappedClass("B"))
            kotlin("src/main/kotlin/com/example/Base.kt", "package com.example\n\nopen class Base(val x: Int)")
            kotlin(
                "src/main/kotlin/com/example/C.kt",
                """
                package com.example

                import me.tbsten.analysis.code.gen.tp.Mapped

                @Mapped
                class C(val base: Base) : Base(1)
                """,
            )
        }
    if (engine == Acg45Engine.Ksp2) fixture.gradleProperties["ksp.incremental"] = "true"
    val task = codegenTask(engine)
    fixture.build(task)
    fixture.change()
    fixture.build(task)
    return lastDirty(probe)
}

class BridgeIncrementalDiffTest : DiffSpec({
    case("BINC-03", "同じ変更に対して KSP2 と同じ dirty なファイル集合を計算する", pending = PendingCase(Milestone.M3)) {
        val changes: List<Pair<String, GradleFixture.() -> Unit>> =
            listOf(
                "isolating の入力を変える" to { kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A", props = "val id: Long")) },
                "上位型を変える" to { kotlin("src/main/kotlin/com/example/Base.kt", "package com.example\n\nopen class Base(val x: Long)") },
                "ファイルを足す" to { kotlin("src/main/kotlin/com/example/D.kt", mappedClass("D")) },
                "ファイルを消す" to { projectDir.resolve("src/main/kotlin/com/example/B.kt").delete() },
            )
        changes.forEach { (name, change) ->
            withClue(name) {
                val expected = dirtyAfterChange(Acg45Engine.Ksp2, change)
                dirtyAfterChange(Acg45Engine.Bridge, change) shouldBe expected
            }
        }
    }
})

private inline fun <T> withClue(
    clue: String,
    block: () -> T,
): T = io.kotest.assertions.withClue(clue, block)

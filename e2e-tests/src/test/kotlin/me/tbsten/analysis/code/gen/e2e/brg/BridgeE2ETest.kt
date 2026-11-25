package me.tbsten.analysis.code.gen.e2e.brg

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import me.tbsten.analysis.code.gen.e2e.support.Acg45Engine
import me.tbsten.analysis.code.gen.e2e.support.Acg45Repo
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
import me.tbsten.analysis.code.gen.testing.runCodegenTest
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files

// 5.14 KSP Bridge の API 互換（BRG）のうち E2E（Gradle TestKit / kct）と、KSP 公式テストスイートの集計（BRG-07）

/** KotlinPoet の KSP 拡張（toClassName・toTypeName・writeTo）だけで書いた Processor。 */
private val poetProvider =
    SymbolProcessorProvider { env ->
        object : SymbolProcessor {
            override fun process(resolver: Resolver): List<KSAnnotated> {
                resolver.getSymbolsWithAnnotation("com.example.Poet").filterIsInstance<KSClassDeclaration>().forEach { cls ->
                    val name = cls.toClassName()
                    val props =
                        cls.getAllProperties().map { p ->
                            PropertySpec
                                .builder(
                                    p.simpleName.asString() + "Type",
                                    String::class,
                                ).initializer(
                                    "%S",
                                    p.type
                                        .resolve()
                                        .toTypeName()
                                        .toString(),
                                ).build()
                        }
                    FileSpec
                        .builder(name.packageName, name.simpleName + "Poet")
                        .addType(
                            TypeSpec
                                .objectBuilder(name.simpleName + "Poet")
                                .addProperties(props.toList())
                                .addFunction(
                                    FunSpec
                                        .builder(
                                            "describe",
                                        ).addParameter(
                                            "value",
                                            cls.asStarProjectedType().toTypeName(),
                                        ).returns(String::class)
                                        .addStatement("return value.toString()")
                                        .build(),
                                ).build(),
                        ).build()
                        .writeTo(env.codeGenerator, Dependencies(false, cls.containingFile!!))
                }
                return emptyList()
            }
        }
    }

private fun brg11(engine: TestEngine) {
    val result =
        runCodegenTest(engine) {
            kotlin(
                "com/example/Poet.kt",
                """
                package com.example

                annotation class Poet

                @Poet
                data class Person(val name: String, val tags: Map<String, List<Int?>>, val friend: Person?)
                """,
            )
            ksp(poetProvider)
        }
    result.isSuccess shouldBe true
    val poet = result.generatedFile("PersonPoet.kt")
    poet shouldContain "kotlin.collections.Map<kotlin.String, kotlin.collections.List<kotlin.Int?>>"
    poet shouldContain "fun describe(`value`: Person): String"
    result.loadClass("com.example.PersonPoet")
}

private fun probeFile(): File = Files.createTempFile("acg-probe", ".jsonl").toFile().apply { delete() }

class BridgeE2ETest : E2ESpec({
    case("BRG-11", "KotlinPoet の KSP 拡張（toTypeName、toClassName、writeTo）が Bridge 上で動く", pending = PendingCase(Milestone.M1)) {
        brg11(TestEngine.AnalysisCodegen)
    }
    "[KSP2 で期待を確認] BRG-11" { brg11(TestEngine.Ksp2) }

    case("BRG-13", "analysisCodeGen { arg(...) } で渡した引数が KSP Processor の options に届く", pending = PendingCase(Milestone.M1)) {
        val probe = probeFile()
        val fixture =
            acg45Project(Acg45Engine.Bridge, processors = "Probe", probeOut = probe, dsl = "arg(\"brg13.key\", \"hello world\")") {
                kotlin("src/main/kotlin/com/example/A.kt", "package com.example\n\nclass A")
            }
        fixture.build(codegenTask(Acg45Engine.Bridge)).task(codegenTask(Acg45Engine.Bridge))?.outcome shouldBe TaskOutcome.SUCCESS
        val create = readProbeLines(probe).first { probeValue(it, "event") == "create" }
        probeValue(create, "frontend") shouldBe "ksp"
        create shouldContain "\"brg13.key\":\"hello world\""
        create shouldContain "\"tp.processors\":\"Probe\""
    }

    case("BRG-14", "Bridge を適用しないプロジェクトの依存に KSP API が含まれない", pending = PendingCase(Milestone.M1)) {
        val fixture = acg45Project(Acg45Engine.Native) { kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A")) }
        // ビルドスクリプトの classpath と、全 Configuration の依存
        val result = fixture.build("buildEnvironment", "dependencies", codegenTask(Acg45Engine.Native))
        result.output shouldNotContain "com.google.devtools.ksp"
        result.output shouldNotContain "symbol-processing"
    }

    case("BRG-15", "KSP の主要な Gradle 設定（増分の有効・無効など）に対応する設定があり、同じ効果がある", pending = PendingCase(Milestone.M3)) {
        // ADR-035: KSP の Gradle プロパティ ksp.incremental=false は Bridge でも同じ意味（毎回全件処理）。
        // ksp.incremental.log=true は analysisCodeGen の増分ログ（build/analysisCodeGen/caches/<unit>/incremental.log）に写す
        val probe = probeFile()
        val fixture =
            acg45Project(Acg45Engine.Bridge, processors = "Mapper,Probe", probeOut = probe) {
                kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A"))
                kotlin("src/main/kotlin/com/example/B.kt", mappedClass("B"))
                gradleProperties["ksp.incremental"] = "false"
                gradleProperties["ksp.incremental.log"] = "true"
            }
        val task = codegenTask(Acg45Engine.Bridge)
        fixture.build(task)
        fixture.kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A", props = "val id: Int, val name: String"))
        fixture.build(task)
        val lastProcess = readProbeLines(probe).last { probeValue(it, "event") == "process" && probeValue(it, "round") == "1" }
        probeArray(
            lastProcess,
            "newFiles",
        ).map { it.substringAfterLast('/') }.filterNot { it == "TpAnnotations.kt" } shouldContainExactlyInAnyOrder
            listOf("A.kt", "B.kt")
        fixture.projectDir.resolve("build/analysisCodeGen/caches/main/incremental.log").readText() shouldContain "ksp.incremental=false"
    }
})

class BridgeSuiteDiffTest : DiffSpec({
    case(
        "BRG-07",
        "KSP 公式テストスイート（KSP2 の AA 実装のテスト一式）を Bridge で実行し、取り込み時に付けた M どおりに通る（下の 6 章）",
        pending = PendingCase(Milestone.M1),
    ) {
        // KSPT-* の 1 件 1 件は ACG-43 が ksp-compatibility-tests にケースとして取り込む。BRG-07 はその集計:
        // M1 に付いた KSPT が 1 件以上あり、すべて有効（保留でない）になっていること。M2・M3 の分は各マイルストーンの合格判定で見る
        val status = File(Acg45Repo.root, "quality/case-status.csv").readLines()
        val header = status.first().split(',')
        val id = header.indexOf("id")
        val milestone = header.indexOf("milestone")
        val state = header.indexOf("status")
        val m1 = status.drop(1).map { it.split(',') }.filter { it[id].startsWith("KSPT-") && it[milestone] == "M1" }
        m1.shouldNotBeEmpty()
        m1.filter { it[state] != "enabled" }.map { it[id] } shouldBe emptyList()
    }
})

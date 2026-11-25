package me.tbsten.analysiscodegen.enginetests.kmp

import io.kotest.matchers.shouldBe
import me.tbsten.analysiscodegen.enginetests.support.names
import me.tbsten.analysiscodegen.enginetests.support.processorProvider
import me.tbsten.analysiscodegen.enginetests.support.runNative
import me.tbsten.analysiscodegen.processor.ProcessingScope
import me.tbsten.analysiscodegen.testfoundation.IntegrationSpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

// 5.2 KMP の整合性（KMP）のうち I のケース。commonMain ← jvmMain の 2 段を runCodegenTest の SourceSet で組む（TST-04）
class KmpIntegrationTest : IntegrationSpec({
    case("KMP-09", "jvmMain の実行で、commonMain の宣言は visibleFiles に入り ownFiles には入らない", PendingCase(Milestone.M1)) {
        var own: List<String>? = null
        var visible: List<String>? = null
        var all: List<String>? = null
        runNative {
            sourceSet("commonMain") { kotlin("c/Common.kt", "package c\nclass Common") }
            sourceSet("jvmMain") {
                dependsOn += "commonMain"
                kotlin("j/Jvm.kt", "package j\nclass Jvm")
            }
            native(
                processorProvider { ctx ->
                    if (ctx.sourceSet.name == "jvmMain" && ctx.round == 1) {
                        own = ctx.sourceSet.ownFiles.names
                        visible = ctx.sourceSet.visibleFiles.names
                        all = ctx.allFiles.names
                    }
                },
            )
        }
        own shouldBe listOf("Jvm.kt")
        visible shouldBe listOf("Common.kt", "Jvm.kt")
        // 既定の Processing Scope は own files
        all shouldBe listOf("Jvm.kt")
    }

    case("KMP-10", "Processing Scope を明示的に広げると上流（commonMain の expect）を処理できるが、出力先は jvmMain のまま", PendingCase(Milestone.M1)) {
        var all: List<String>? = null
        var owner: String? = null
        val result =
            runNative {
                sourceSet("commonMain") { kotlin("c/Common.kt", "package c\nexpect fun platform(): String") }
                sourceSet("jvmMain") {
                    dependsOn += "commonMain"
                    processingScope = ProcessingScope.IncludeDependsOn
                    kotlin("j/Jvm.kt", "package j\nclass Jvm")
                }
                native(
                    processorProvider { ctx ->
                        if (ctx.sourceSet.name != "jvmMain" || ctx.round != 1) return@processorProvider
                        all = ctx.allFiles.names
                        owner = ctx.executionUnit.name
                        // 上流の expect を見て actual を作る
                        val expects =
                            ctx.allFiles
                                .flatMap { f ->
                                    f.collectDescendantsOfType<KtNamedFunction>()
                                }.filter { it.hasModifier(org.jetbrains.kotlin.lexer.KtTokens.EXPECT_KEYWORD) }
                        expects.forEach { fn ->
                            ctx.codeGenerator.generateKotlin(
                                "c",
                                "${fn.name}Actual",
                                me.tbsten.analysiscodegen.processor.Dependencies.all,
                                "package c\nactual fun ${fn.name}(): String = \"jvm\"",
                            )
                        }
                    },
                )
            }
        all shouldBe listOf("Common.kt", "Jvm.kt")
        result.isSuccess shouldBe true
        // 出力の所有者は jvmMain の実行単位（ADR-013）
        owner shouldBe "jvmMain"
        result.generatedFiles.keys shouldBe setOf("kotlin/c/platformActual.kt")
    }

    case("KMP-15", "テストからの friend 依存: commonTest の解析で commonMain の internal が解決される", PendingCase(Milestone.M2)) {
        var resolved: Boolean? = null
        runNative {
            sourceSet("commonMain") { kotlin("c/Hidden.kt", "package c\ninternal class Hidden") }
            sourceSet("commonTest") {
                friendOf += "commonMain"
                kotlin("c/HiddenTest.kt", "package c\nclass HiddenTest { val h = Hidden() }")
            }
            native(
                processorProvider { ctx ->
                    if (ctx.sourceSet.name != "commonTest" || ctx.round != 1) return@processorProvider
                    val property =
                        ctx.allFiles
                            .single()
                            .collectDescendantsOfType<org.jetbrains.kotlin.psi.KtProperty>()
                            .single()
                    resolved =
                        analyze(property) {
                            (property.returnType as? org.jetbrains.kotlin.analysis.api.types.KaClassType)?.classId?.asFqNameString() ==
                                "c.Hidden"
                        }
                },
            )
        }
        resolved shouldBe true
    }

    case("KMP-16", "jvmMain の解析で expect 宣言から対応する actual を AA で辿れる", PendingCase(Milestone.M2)) {
        var actualFiles: List<String>? = null
        var expectFiles: List<String>? = null
        runNative {
            sourceSet("commonMain") { kotlin("c/Expect.kt", "package c\nexpect fun platform(): String\nexpect class Clock()") }
            sourceSet("jvmMain") {
                dependsOn += "commonMain"
                kotlin("c/Actual.kt", "package c\nactual fun platform(): String = \"jvm\"\nactual class Clock actual constructor()")
            }
            native(
                processorProvider { ctx ->
                    if (ctx.sourceSet.name != "jvmMain" || ctx.round != 1) return@processorProvider
                    analyze(ctx.sourceSet.module) {
                        // jvmMain のモジュールで名前を引くと actual が見つかり、そこから expect に戻れる
                        val fn = findTopLevelCallables(FqName("c"), Name.identifier("platform")).filter { it.isActual }.toList()
                        val cls = findClass(ClassId(FqName("c"), Name.identifier("Clock")))!!
                        actualFiles = (fn + cls).map { it.psi!!.containingFile.name }
                        expectFiles = (fn + cls).flatMap { it.getExpectsForActual() }.map { it.psi!!.containingFile.name }
                    }
                },
            )
        }
        actualFiles shouldBe listOf("Actual.kt", "Actual.kt")
        expectFiles shouldBe listOf("Expect.kt", "Expect.kt")
    }
})

package me.tbsten.analysis.code.gen.enginetests.rnd

import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysis.code.gen.enginetests.support.names
import me.tbsten.analysis.code.gen.enginetests.support.processorProvider
import me.tbsten.analysis.code.gen.enginetests.support.runNative
import me.tbsten.analysis.code.gen.processor.Dependencies
import me.tbsten.analysis.code.gen.testfoundation.IntegrationSpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

// 5.5 マルチラウンド（RND）のうち I のケース
class RoundsIntegrationTest : IntegrationSpec({
    case("RND-01", "ラウンド 1 の生成物をラウンド 2 で解析・型解決できる", PendingCase(Milestone.M1)) {
        var resolvedInRound2: String? = null
        val result =
            runNative {
                kotlin("a/A.kt", "package a\nclass A")
                native(
                    processorProvider { ctx ->
                        when (ctx.round) {
                            1 -> ctx.codeGenerator.generateKotlin("a", "Gen", Dependencies.all, "package a\nclass Gen { val a: A = A() }")
                            2 -> {
                                val prop =
                                    ctx.newFiles
                                        .single()
                                        .collectDescendantsOfType<KtProperty>()
                                        .single()
                                resolvedInRound2 = analyze(prop) { (prop.returnType as? KaClassType)?.classId?.asFqNameString() }
                            }
                        }
                    },
                )
            }
        result.isSuccess shouldBe true
        resolvedInRound2 shouldBe "a.A"
    }

    case("RND-03", "同じラウンド内では他の Processor の生成物が見えない（スナップショット固定）", PendingCase(Milestone.M1)) {
        val seenByB = mutableMapOf<Int, Pair<List<String>, Boolean>>()
        runNative {
            kotlin("a/A.kt", "package a\nclass A")
            // 登録順で A が先に動き、生成した直後に B が動く
            native(
                processorProvider { ctx ->
                    if (ctx.round ==
                        1
                    ) {
                        ctx.codeGenerator.generateKotlin("a", "FromA", Dependencies.all, "package a\nclass FromA")
                    }
                },
            )
            native(
                processorProvider { ctx ->
                    val found = analyze(ctx.sourceSet.module) { findClass(ClassId(FqName("a"), Name.identifier("FromA"))) != null }
                    seenByB[ctx.round] = ctx.allFiles.names to found
                },
            )
        }
        seenByB.getValue(1) shouldBe (listOf("A.kt") to false)
        seenByB.getValue(2) shouldBe (listOf("A.kt", "FromA.kt") to true)
    }

    case("RND-04", "newFiles に入るのは前のラウンドの生成物だけ", PendingCase(Milestone.M1)) {
        val newFilesByRound = mutableMapOf<Int, List<String>>()
        runNative {
            kotlin("a/A.kt", "package a\nclass A")
            native(
                processorProvider { ctx ->
                    newFilesByRound[ctx.round] = ctx.newFiles.names
                    if (ctx.round <=
                        2
                    ) {
                        ctx.codeGenerator.generateKotlin("a", "R${ctx.round}", Dependencies.all, "package a\nclass R${ctx.round}")
                    }
                },
            )
            // 別の Processor の生成物も「前のラウンドの生成物」として次のラウンドの newFiles に入る（自分の分だけではない）
            native(
                processorProvider { ctx ->
                    if (ctx.round ==
                        1
                    ) {
                        ctx.codeGenerator.generateKotlin("a", "Other", Dependencies.all, "package a\nclass Other")
                    }
                },
            )
        }
        newFilesByRound shouldBe
            mapOf(
                1 to listOf("A.kt"),
                2 to listOf("Other.kt", "R1.kt"),
                3 to listOf("R2.kt"),
            )
    }

    case("RND-05", "出力の検証に失敗したラウンドの生成物は解析対象に入らない（Stage → Validate → Commit）", PendingCase(Milestone.M2)) {
        val rounds = mutableListOf<Int>()
        val result =
            runNative {
                kotlin("a/A.kt", "package a\nclass A")
                // 既存ソースと同じ FQN（a.A）を生成して Validate を失敗させる（TRM-07）
                native(
                    processorProvider { ctx ->
                        rounds += ctx.round
                        if (ctx.round == 1) {
                            ctx.codeGenerator.generateKotlin("a", "Ok", Dependencies.all, "package a\nclass Ok")
                            ctx.codeGenerator.generateKotlin("a", "Dup", Dependencies.all, "package a\nclass A")
                        }
                    },
                )
            }
        result.isSuccess shouldBe false
        // 次のラウンドに進まず、同じラウンドの他の生成物（Ok）も出力に残らない
        rounds shouldBe listOf(1)
        result.generatedFiles.keys shouldNotContain "kotlin/a/Ok.kt"
        result.errors.single().message shouldContain "a.A"
    }

    case("RND-06", "前のラウンドで生成した Java ソースを、次のラウンドの Kotlin 解析で解決できる", PendingCase(Milestone.M2)) {
        var javaSeen: List<String>? = null
        var resolved: String? = null
        val result =
            runNative {
                kotlin("a/A.kt", "package a\nclass A")
                native(
                    processorProvider { ctx ->
                        when (ctx.round) {
                            1 -> {
                                ctx.codeGenerator.generateJava(
                                    "a",
                                    "J",
                                    Dependencies.all,
                                    "package a;\npublic class J { public static String hi() { return \"hi\"; } }",
                                )
                                ctx.codeGenerator.generateKotlin("a", "UsesJ", Dependencies.all, "package a\nval j: J = J()")
                            }
                            2 -> {
                                javaSeen = ctx.allJavaFiles.map { it.name }
                                val prop =
                                    ctx.newFiles
                                        .single()
                                        .collectDescendantsOfType<KtProperty>()
                                        .single()
                                resolved = analyze(prop) { (prop.returnType as? KaClassType)?.classId?.asFqNameString() }
                            }
                        }
                    },
                )
            }
        result.isSuccess shouldBe true
        javaSeen shouldBe listOf("J.java")
        resolved shouldBe "a.J"
    }
})

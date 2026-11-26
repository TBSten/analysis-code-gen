package me.tbsten.analysis.code.gen.enginetests.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysis.code.gen.enginetests.support.KSP_SERVICE
import me.tbsten.analysis.code.gen.enginetests.support.NATIVE_SERVICE
import me.tbsten.analysis.code.gen.enginetests.support.generateOnceProvider
import me.tbsten.analysis.code.gen.enginetests.support.names
import me.tbsten.analysis.code.gen.enginetests.support.processorProvider
import me.tbsten.analysis.code.gen.enginetests.support.runNative
import me.tbsten.analysis.code.gen.enginetests.support.serviceJar
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessor
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorProvider
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysis.code.gen.processor.Dependencies
import me.tbsten.analysis.code.gen.testfoundation.IntegrationSpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import me.tbsten.analysis.code.gen.testfoundation.UnitSpec
import org.jetbrains.kotlin.platform.jvm.isJvm

private val M1 = PendingCase(Milestone.M1)
private val M2 = PendingCase(Milestone.M2)

/** serviceJar の META-INF/services に書いて ServiceLoader で探させる Provider（API-01）。 */
class ServiceLoadedProvider : AnalysisCodegenProcessorProvider by generateOnceProvider(
    "svc",
    "FromServiceLoader",
    "package svc\nobject FromServiceLoader",
)

// 5.3 Native Processor API（API）のうち I のケース。エンジンを同じプロセスで動かし、Processor が見たものを記録して確かめる
class NativeApiIntegrationTest : IntegrationSpec({
    case("API-01", "ServiceLoader で AnalysisCodegenProcessorProvider が見つかり、Processor が生成される", M1) {
        // Processor の JAR（component）の META-INF/services にだけ書いた Provider を、エンジンが ServiceLoader で探す
        val jar = serviceJar(mapOf(NATIVE_SERVICE to listOf(ServiceLoadedProvider::class.java.name)))
        val result =
            runNative {
                kotlin("a/A.kt", "package a\nclass A")
                processorJar(jar)
            }
        result.isSuccess shouldBe true
        result.generatedFile("FromServiceLoader.kt") shouldContain "object FromServiceLoader"
    }

    case("API-02", "複数の Processor が同じラウンドで全部実行される", M1) {
        val calls = mutableListOf<Pair<String, Int>>()
        val result =
            runNative {
                kotlin("a/A.kt", "package a\nclass A")
                listOf("p1", "p2", "p3").forEach { name -> native(processorProvider { calls += name to it.round }) }
            }
        result.isSuccess shouldBe true
        calls.filter { it.second == 1 }.map { it.first }.sorted() shouldBe listOf("p1", "p2", "p3")
    }

    case("API-03", "オプションが Provider と RoundContext に渡る", M1) {
        var fromEnv: Map<String, String>? = null
        var fromRound: Map<String, String>? = null
        runNative {
            kotlin("a/A.kt", "package a\nclass A")
            options["k1"] = "v1"
            options["k2"] = "v2"
            options["analysisCodeGen.internal.shuffleSeed"] = "1"
            native(processorProvider(onCreate = { fromEnv = it.options }) { fromRound = it.options })
        }
        fromEnv shouldBe mapOf("k1" to "v1", "k2" to "v2")
        // 予約キー（analysisCodeGen.）は Processor に渡らない（ADR-013）
        fromRound shouldBe fromEnv
    }

    case("API-05", "CodeGenerator で Kotlin・Java・リソース（META-INF/services など）を生成でき、パッケージとパスが一致する", M1) {
        val result =
            runNative {
                kotlin("a/A.kt", "package a\nclass A")
                native(
                    processorProvider { ctx ->
                        if (ctx.round != 1) return@processorProvider
                        val deps = Dependencies.aggregating(ctx.allFiles)
                        ctx.codeGenerator.generateKotlin("x.y", "K", deps, "package x.y\nobject K")
                        ctx.codeGenerator.generateJava("x.y", "J", deps, "package x.y;\npublic class J {}")
                        ctx.codeGenerator.generateResource("META-INF/services/x.y.Svc", deps, "x.y.Impl\n".toByteArray())
                    },
                )
            }
        result.isSuccess shouldBe true
        result.generatedFiles.keys shouldBe setOf("java/x/y/J.java", "kotlin/x/y/K.kt", "resources/META-INF/services/x.y.Svc")
        result.generatedFiles.getValue("resources/META-INF/services/x.y.Svc") shouldBe "x.y.Impl\n"
        result.loadClass("x.y.J").simpleName shouldBe "J"
    }

    case("API-07", "RoundContext の round / allFiles / newFiles / changedFiles が定義どおりの集合になる", M1) {
        data class Seen(val round: Int, val all: List<String>, val new: List<String>, val changed: List<String>, val removed: Set<String>)
        val seen = mutableListOf<Seen>()
        runNative {
            kotlin("a/B.kt", "package a\nclass B")
            kotlin("a/A.kt", "package a\nclass A")
            native(
                processorProvider { ctx ->
                    seen += Seen(ctx.round, ctx.allFiles.names, ctx.newFiles.names, ctx.changedFiles.names, ctx.removedFilePaths)
                    if (ctx.round == 1) ctx.codeGenerator.generateKotlin("a", "Gen", Dependencies.all, "package a\nclass Gen")
                },
            )
        }
        // 全件ビルド: ラウンド 1 の newFiles = allFiles、changed / removed は空。どれもパスの辞書順
        seen[0] shouldBe Seen(1, listOf("A.kt", "B.kt"), listOf("A.kt", "B.kt"), emptyList(), emptySet())
        // ラウンド 2: allFiles に前のラウンドの生成物が入り、newFiles はその生成物だけ
        seen[1] shouldBe Seen(2, listOf("A.kt", "B.kt", "Gen.kt"), listOf("Gen.kt"), emptyList(), emptySet())
        seen.size shouldBe 2
    }

    case("API-08", "SourceSetContext の name / platform / ownFiles / visibleFiles / dependencies / module が正しい", M1) {
        data class Seen(
            val name: String,
            val jvm: Boolean,
            val own: List<String>,
            val visible: List<String>,
            val deps: List<String>,
            val module: String,
        )
        val seen = mutableListOf<Seen>()
        runNative {
            sourceSet("commonMain") { kotlin("c/C.kt", "package c\nclass C") }
            sourceSet("jvmMain") {
                dependsOn += "commonMain"
                kotlin("j/J.kt", "package j\nclass J")
            }
            native(
                processorProvider { ctx ->
                    val ss = ctx.sourceSet
                    if (ss.name == "jvmMain") {
                        seen +=
                            Seen(
                                ss.name,
                                ss.platform.isJvm(),
                                ss.ownFiles.names,
                                ss.visibleFiles.names,
                                ss.dependencies.map { it.name },
                                ss.module.name,
                            )
                    }
                },
            )
        }
        seen.first() shouldBe Seen("jvmMain", true, listOf("J.kt"), listOf("C.kt", "J.kt"), listOf("commonMain"), "jvmMain")
    }

    case("API-09", "finish() は全ラウンド後に 1 回だけ、onError() はエラー時だけ呼ばれる", M1) {
        val okEvents = mutableListOf<String>()
        runNative {
            kotlin("a/A.kt", "package a\nclass A")
            native(
                processorProvider(onFinish = { okEvents += "finish" }, onError = { okEvents += "onError" }) { ctx ->
                    okEvents += "process${ctx.round}"
                    if (ctx.round == 1) ctx.codeGenerator.generateKotlin("a", "G", Dependencies.all, "package a\nclass G")
                },
            )
        }.isSuccess shouldBe true
        okEvents shouldBe listOf("process1", "process2", "finish")

        val errorEvents = mutableListOf<String>()
        runNative {
            kotlin("a/A.kt", "package a\nclass A")
            native(
                processorProvider(onFinish = { errorEvents += "finish" }, onError = { errorEvents += "onError" }) { ctx ->
                    errorEvents += "process${ctx.round}"
                    ctx.logger.error("boom")
                },
            )
        }.isSuccess shouldBe false
        errorEvents shouldBe listOf("process1", "onError")
    }

    case("API-10", "Processor から解析環境を書き換える操作をすると、原因が分かる例外になる（Processor には読み取りだけを公開）", M2) {
        val result =
            runNative {
                kotlin("a/A.kt", "package a\nclass A")
                native(
                    processorProvider { ctx ->
                        // PSI を書き換える（読み取りしか許さない）
                        val file = ctx.allFiles.single()
                        file.declarations.single().delete()
                    },
                )
            }
        result.isSuccess shouldBe false
        val message = result.errors.joinToString("\n") { it.message }
        message shouldContain "書き換え"
        message shouldContain "NativeApiIntegrationTest"
        result.generatedFiles.keys.shouldBeEmpty()
    }

    case("API-11", "Provider や Processor が例外を投げたら、Processor 名とスタックトレース付きでビルドが失敗する", M1) {
        val fromProcess =
            runNative {
                kotlin("a/A.kt", "package a\nclass A")
                native(ThrowingProvider())
            }
        fromProcess.isSuccess shouldBe false
        fromProcess.messages shouldContain ThrowingProvider::class.java.name
        fromProcess.messages shouldContain "IllegalStateException: from process"
        fromProcess.messages shouldContain "at ${ThrowingProvider::class.java.name}"

        val fromProvider =
            runNative {
                kotlin("a/A.kt", "package a\nclass A")
                native(AnalysisCodegenProviderThatThrows())
            }
        fromProvider.isSuccess shouldBe false
        fromProvider.messages shouldContain AnalysisCodegenProviderThatThrows::class.java.name
        fromProvider.messages shouldContain "from create"
    }

    case("API-12", "Processor インスタンスが実行単位の間で同じもの（ラウンドごとに作り直されない）", M1) {
        var creates = 0
        val instances = mutableSetOf<Int>()
        runNative {
            kotlin("a/A.kt", "package a\nclass A")
            native(
                AnalysisCodegenProcessorProvider {
                    creates++
                    object : AnalysisCodegenProcessor {
                        override fun process(context: AnalysisCodegenRoundContext) {
                            instances += System.identityHashCode(this)
                            if (context.round < 3) {
                                context.codeGenerator.generateKotlin(
                                    "a",
                                    "G${context.round}",
                                    Dependencies.all,
                                    "package a\nclass G${context.round}",
                                )
                            }
                        }
                    }
                },
            )
        }
        creates shouldBe 1
        instances.size shouldBe 1
    }

    case("API-14", "Native と KSP の Provider を両方含む JAR で、ADR で決めた規則どおりに片方だけ実行される（二重実行しない）", M2) {
        // ADR-050: KSP の Provider しか無い component は Bridge を自動で有効にして KSP として動かす（plugin は 1 つ）。
        // ADR-017: 同じ component に Native の Provider があれば Native だけを使い、KSP の Provider は使わず info を出す
        val jar =
            serviceJar(
                mapOf(
                    NATIVE_SERVICE to listOf(DualNativeProvider::class.java.name),
                    KSP_SERVICE to listOf(DualKspProvider::class.java.name),
                ),
            )
        val result =
            runNative {
                kotlin("a/A.kt", "package a\nclass A")
                processorJar(jar)
            }
        result.isSuccess shouldBe true
        result.generatedFiles.keys shouldBe setOf("kotlin/dual/FromNative.kt")
        result.diagnostics.any { it.message.contains(DualKspProvider::class.java.name) } shouldBe true
    }
})

// 5.3 のうち U のケース。CodeGenerator の実装は公開 API 越しにしか触れないため、エンジンを通して呼び出し時の振る舞いを見る
class NativeApiUnitTest : UnitSpec({
    case("API-06", "出力ルートの外（../、絶対パス）への書き込みが拒否される", M1) {
        val thrown = mutableListOf<Throwable>()
        runNative {
            kotlin("a/A.kt", "package a\nclass A")
            native(
                processorProvider { ctx ->
                    if (ctx.round != 1) return@processorProvider
                    for (call in listOf<() -> Unit>(
                        { ctx.codeGenerator.generateResource("../escape.txt", Dependencies.all, byteArrayOf()) },
                        { ctx.codeGenerator.generateResource("/tmp/abs.txt", Dependencies.all, byteArrayOf()) },
                        { ctx.codeGenerator.generateKotlin("..", "Up", Dependencies.all, "") },
                    )) {
                        thrown += shouldThrow<IllegalArgumentException> { call() }
                    }
                },
            )
        }
        thrown.size shouldBe 3
    }

    case("API-13", "生成ファイルの文字コードが UTF-8、改行が LF に固定される", M1) {
        val result =
            runNative {
                kotlin("a/A.kt", "package a\nclass A")
                native(
                    processorProvider { ctx ->
                        if (ctx.round == 1) {
                            ctx.codeGenerator.generateKotlin("a", "Crlf", Dependencies.all, "package a\r\n// 日本語のコメント\r\nobject Crlf\r\n")
                        }
                    },
                )
            }
        // generatedFiles は出力を UTF-8 として読んだもの。CR が残っていれば文字列に現れる
        result.generatedFiles.getValue("kotlin/a/Crlf.kt") shouldBe "package a\n// 日本語のコメント\nobject Crlf\n"
    }
})

private class ThrowingProvider : AnalysisCodegenProcessorProvider by processorProvider(process = { error("from process") })

private class AnalysisCodegenProviderThatThrows : AnalysisCodegenProcessorProvider {
    override fun create(environment: me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorEnvironment) = error("from create")
}

/** API-14: 同じ JAR に置く Native 側。 */
class DualNativeProvider : AnalysisCodegenProcessorProvider by generateOnceProvider("dual", "FromNative", "package dual\nobject FromNative")

/** API-14: 同じ JAR に置く KSP 側。実行されたら FromKsp を作る（作られてはいけない）。 */
class DualKspProvider : com.google.devtools.ksp.processing.SymbolProcessorProvider {
    override fun create(environment: com.google.devtools.ksp.processing.SymbolProcessorEnvironment) =
        object : com.google.devtools.ksp.processing.SymbolProcessor {
            private var done = false

            override fun process(resolver: com.google.devtools.ksp.processing.Resolver): List<com.google.devtools.ksp.symbol.KSAnnotated> {
                if (!done) {
                    done = true
                    environment.codeGenerator
                        .createNewFile(
                            com.google.devtools.ksp.processing
                                .Dependencies(false),
                            "dual",
                            "FromKsp",
                        ).use { it.write("package dual\nobject FromKsp\n".toByteArray()) }
                }
                return emptyList()
            }
        }
}

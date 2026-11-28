package me.tbsten.analysis.code.gen.enginetests.trm

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysis.code.gen.enginetests.support.processorProvider
import me.tbsten.analysis.code.gen.enginetests.support.runNative
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorProvider
import me.tbsten.analysis.code.gen.processor.Dependencies
import me.tbsten.analysis.code.gen.testfoundation.IntegrationSpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

private val M1 = PendingCase(Milestone.M1)

/** 毎ラウンド新しいファイルを作り続ける（TRM-01）。 */
class InfiniteProvider : AnalysisCodegenProcessorProvider by processorProvider(process = { ctx ->
    ctx.codeGenerator.generateKotlin("inf", "Gen${ctx.round}", Dependencies.all, "package inf\nclass Gen${ctx.round}")
})

/** 毎ラウンド同じクラスを defer し続け、毎ラウンド新しいファイルも作る（TRM-02）。 */
class StuckDeferProvider : AnalysisCodegenProcessorProvider by processorProvider(process = { ctx ->
    val target = ctx.allFiles.flatMap { it.collectDescendantsOfType<KtClass>() }.first { it.name == "Target" }
    analyze(target) {
        @Suppress("UNCHECKED_CAST")
        ctx.defer(target.symbol.createPointer() as KaSymbolPointer<KaSymbol>, "stuck")
    }
    ctx.codeGenerator.generateKotlin("stuck", "Progress${ctx.round}", Dependencies.all, "package stuck\nclass Progress${ctx.round}")
})

/** same/Same.kt に書くだけの Provider（TRM-03・04）。 */
private fun writer(
    times: Int = 1,
    inRounds: Set<Int> = setOf(1),
) = processorProvider { ctx ->
    if (ctx.round in
        inRounds
    ) {
        repeat(times) { ctx.codeGenerator.generateKotlin("same", "Same", Dependencies.all, "package same\nclass Same") }
    }
}

// 5.7 停止と出力の検証（TRM）のうち I のケース（ADR-014 の表）
class TerminationIntegrationTest : IntegrationSpec({
    case("TRM-01", "毎ラウンド新しいファイルを作り続ける Processor を最大ラウンド数で止め、Processor 名とラウンド数を診断に出す") {
        val result =
            runNative {
                kotlin("a/A.kt", "package a\nclass A")
                maxRounds = 10
                native(InfiniteProvider())
            }
        result.isSuccess shouldBe false
        val message = result.errors.single().message
        message shouldContain "10"
        message shouldContain InfiniteProvider::class.java.name
        message shouldContain "inf/Gen10.kt"
    }

    case("TRM-02", "同じ deferred が進まずに繰り返されたら止めて診断する") {
        // 進捗（新しいファイル）があるあいだは maxRounds まで進み、そこで deferred の一覧付きで止まる。
        // 新しいファイルが無くなって deferred だけが残った場合は DEF-04
        val result =
            runNative {
                kotlin("a/Target.kt", "package a\nclass Target")
                maxRounds = 8
                native(StuckDeferProvider())
            }
        result.isSuccess shouldBe false
        val message = result.errors.joinToString("\n") { it.message }
        message shouldContain StuckDeferProvider::class.java.name
        message shouldContain "a.Target"
        message shouldContain "8"
    }

    case("TRM-03", "異なる Processor が同じ出力先に書いたらエラー") {
        val sameRound =
            runNative {
                kotlin("a/A.kt", "package a\nclass A")
                native(writer())
                native(writer())
            }
        sameRound.isSuccess shouldBe false
        sameRound.errors.single().message shouldContain "kotlin/same/Same.kt"

        // 別のラウンドでも同じ（ADR-014）
        val laterRound =
            runNative {
                kotlin("a/A.kt", "package a\nclass A")
                native(writer())
                native(
                    processorProvider { ctx ->
                        if (ctx.round ==
                            1
                        ) {
                            ctx.codeGenerator.generateKotlin("x", "Tick", Dependencies.all, "package x\nclass Tick")
                        }
                    },
                )
                native(writer(inRounds = setOf(2)))
            }
        laterRound.isSuccess shouldBe false
        laterRound.errors.single().message shouldContain "kotlin/same/Same.kt"
    }

    case("TRM-04", "同じ Processor が同じファイルを 2 回作ったらエラー（KSP 互換モードでは KSP と同じ例外）", M1) {
        val result =
            runNative {
                kotlin("a/A.kt", "package a\nclass A")
                native(writer(times = 2))
            }
        result.isSuccess shouldBe false
        result.errors.single().message shouldContain "kotlin/same/Same.kt"

        // KSP 互換（Bridge）では 2 回目の createNewFile で KSP と同じ FileAlreadyExistsException
        val kspResult =
            runNative {
                kotlin("a/A.kt", "package a\nclass A")
                ksp(DuplicateKspProvider())
            }
        kspResult.isSuccess shouldBe false
        kspResult.messages shouldContain "FileAlreadyExistsException"
        DuplicateKspProvider.secondCallThrew shouldBe true
    }

    case("TRM-05", "内容が同じ再生成を進捗として数えない（それだけで次のラウンドに進まない）") {
        val rounds = mutableListOf<Int>()
        val result =
            runNative {
                kotlin("a/A.kt", "package a\nclass A")
                native(
                    processorProvider { ctx ->
                        rounds += ctx.round
                        // 毎ラウンド同じパスに同じ内容を書く。2 回目以降は何もしない扱い（info）
                        ctx.codeGenerator.generateKotlin("same", "Same", Dependencies.all, "package same\nclass Same")
                    },
                )
            }
        result.isSuccess shouldBe true
        rounds shouldBe listOf(1, 2)
        result.diagnostics.any { it.message.contains("kotlin/same/Same.kt") } shouldBe true
    }

    case("TRM-06", "他の SourceSet の出力ディレクトリや上流の生成物への書き込みを拒否する") {
        val thrown = mutableListOf<String>()
        runNative {
            sourceSet("commonMain") { kotlin("c/C.kt", "package c\nclass C") }
            sourceSet("jvmMain") {
                dependsOn += "commonMain"
                kotlin("j/J.kt", "package j\nclass J")
            }
            native(
                processorProvider { ctx ->
                    when (ctx.sourceSet.name) {
                        "commonMain" ->
                            if (ctx.round ==
                                1
                            ) {
                                ctx.codeGenerator.generateKotlin("c", "Up", Dependencies.all, "package c\nclass Up")
                            }
                        "jvmMain" ->
                            if (ctx.round == 1) {
                                // 上流（commonMain）の生成物と、他の実行単位の出力ディレクトリへの相対パス
                                for (path in listOf("../../commonMain/kotlin/c/Up.kt", "../commonMain/resources/x.txt")) {
                                    thrown +=
                                        shouldThrow<IllegalArgumentException> {
                                            ctx.codeGenerator.generateResource(
                                                path,
                                                Dependencies.all,
                                                byteArrayOf(),
                                            )
                                        }.message.orEmpty()
                                }
                            }
                    }
                },
            )
        }
        thrown.size shouldBe 2
    }
})

/** TRM-04 の KSP 側: 同じファイルを 2 回作る。 */
class DuplicateKspProvider : com.google.devtools.ksp.processing.SymbolProcessorProvider {
    override fun create(environment: com.google.devtools.ksp.processing.SymbolProcessorEnvironment) =
        object : com.google.devtools.ksp.processing.SymbolProcessor {
            private var done = false

            override fun process(resolver: com.google.devtools.ksp.processing.Resolver): List<com.google.devtools.ksp.symbol.KSAnnotated> {
                if (done) return emptyList()
                done = true
                val gen = environment.codeGenerator
                gen
                    .createNewFile(
                        com.google.devtools.ksp.processing
                            .Dependencies(false),
                        "dup",
                        "Dup",
                    ).close()
                try {
                    gen
                        .createNewFile(
                            com.google.devtools.ksp.processing
                                .Dependencies(false),
                            "dup",
                            "Dup",
                        ).close()
                } catch (e: kotlin.io.FileAlreadyExistsException) {
                    secondCallThrew = true
                    throw e
                }
                return emptyList()
            }
        }

    companion object {
        @Volatile var secondCallThrew: Boolean = false
    }
}

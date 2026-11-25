package me.tbsten.analysiscodegen.enginetests.def

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysiscodegen.enginetests.support.processorProvider
import me.tbsten.analysiscodegen.enginetests.support.runNative
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessor
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorProvider
import me.tbsten.analysiscodegen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysiscodegen.processor.Dependencies
import me.tbsten.analysiscodegen.testfoundation.IntegrationSpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import me.tbsten.analysiscodegen.testing.TestEngine
import me.tbsten.analysiscodegen.testing.runCodegenScenario
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

private val M1 = PendingCase(Milestone.M1)
private val M2 = PendingCase(Milestone.M2)

/**
 * `@Needs("<FQN>")` の代わりに、クラス名が `Needs<Name>` のクラスを見て `<Name>` が解決できるまで defer する。
 * 解決できたら `<Name>Seen` を作る（TP-Defer の小型版）。
 */
private fun deferUntilResolvedProvider(log: MutableList<String>) =
    processorProvider { ctx ->
        val candidates =
            ctx.newFiles.flatMap { it.collectDescendantsOfType<KtClass>() }.filter { it.name!!.startsWith("Needs") }
        analyze(ctx.sourceSet.module) {
            val fromSource = candidates.map { it.symbol as KaClassSymbol }
            val fromDeferred = ctx.deferred.map { it.pointer.restoreSymbol() as KaClassSymbol }
            for (symbol in fromSource + fromDeferred) {
                val target = symbol.name!!.asString().removePrefix("Needs")
                if (findClass(ClassId(FqName("d"), Name.identifier(target))) == null) {
                    log += "defer $target in round ${ctx.round}"
                    @Suppress("UNCHECKED_CAST")
                    ctx.defer(symbol.createPointer() as KaSymbolPointer<KaSymbol>, target)
                } else {
                    log += "resolve $target in round ${ctx.round}"
                    ctx.codeGenerator.generateKotlin("d", "${target}Seen", Dependencies.all, "package d\nobject ${target}Seen")
                }
            }
        }
    }

/** ラウンド [round] で d.<name> を生成する。 */
private fun generateAt(
    round: Int,
    name: String,
) = processorProvider { ctx ->
    if (ctx.round ==
        round
    ) {
        ctx.codeGenerator.generateKotlin("d", name, Dependencies.all, "package d\nclass $name")
    }
}

// 5.6 Deferred と Lifetime（DEF / LIF）。ADR-014「Deferred と Lifetime」
class DeferredLifetimeIntegrationTest : IntegrationSpec({
    case("DEF-01", "未解決のシンボルを defer し、次のラウンドで生成物によって解決されて処理される", M1) {
        val log = mutableListOf<String>()
        val result =
            runNative {
                kotlin("d/NeedsLater.kt", "package d\nclass NeedsLater")
                // 自分自身がラウンド 1 で d.Later を生成し、ラウンド 2 で解決する
                native(deferUntilResolvedProvider(log))
                native(generateAt(1, "Later"))
            }
        result.isSuccess shouldBe true
        log shouldBe listOf("defer Later in round 1", "resolve Later in round 2")
        result.generatedFile("LaterSeen.kt") shouldContain "object LaterSeen"
    }

    case("DEF-02", "KaSymbolPointer がラウンドをまたいで復元できる", M1) {
        val restored = mutableListOf<String>()
        var payload: String? = null
        runNative {
            kotlin("d/Keep.kt", "package d\nclass Keep { fun f() = 1 }")
            native(
                processorProvider { ctx ->
                    when (ctx.round) {
                        1 -> {
                            val cls =
                                ctx.allFiles
                                    .single()
                                    .collectDescendantsOfType<KtClass>()
                                    .single()
                            analyze(cls) {
                                @Suppress("UNCHECKED_CAST")
                                ctx.defer(cls.symbol.createPointer() as KaSymbolPointer<KaSymbol>, "kept")
                            }
                            // 次のラウンドに進むため 1 つ生成する
                            ctx.codeGenerator.generateKotlin("d", "Tick", Dependencies.all, "package d\nclass Tick")
                        }
                        2 -> {
                            payload = ctx.deferred.single().payload
                            analyze(ctx.sourceSet.module) {
                                val symbol =
                                    ctx.deferred
                                        .single()
                                        .pointer
                                        .restoreSymbol() as KaClassSymbol
                                restored += symbol.classId!!.asFqNameString()
                                restored +=
                                    symbol.memberScope.callables
                                        .map { it.callableId?.callableName?.asString() }
                                        .filterNotNull()
                                        .filter {
                                            it ==
                                                "f"
                                        }.toList()
                            }
                        }
                    }
                },
            )
        }
        payload shouldBe "kept"
        restored shouldBe listOf("d.Keep", "f")
    }

    case("DEF-03", "復元できない Pointer（元の宣言が消えた）を ADR どおりに扱い、診断を出す", M2) {
        // メモリ上だけのファイル（KtPsiFactory）の宣言はラウンドをまたぐと消える。ADR-014: 落として warning を出し、ビルドは続ける
        var deferredInRound2: Int? = null
        val result =
            runNative {
                kotlin("d/A.kt", "package d\nclass A")
                native(
                    processorProvider { ctx ->
                        when (ctx.round) {
                            1 -> {
                                val source = ctx.allFiles.single()
                                val ghostFile: KtFile = KtPsiFactory(source.project).createFile("Ghost.kt", "package d\nclass Ghost")
                                val ghost = ghostFile.collectDescendantsOfType<KtClass>().single()
                                analyze(ghost) {
                                    @Suppress("UNCHECKED_CAST")
                                    ctx.defer(ghost.symbol.createPointer() as KaSymbolPointer<KaSymbol>, "ghost")
                                }
                                ctx.codeGenerator.generateKotlin("d", "Tick", Dependencies.all, "package d\nclass Tick")
                            }
                            2 -> deferredInRound2 = ctx.deferred.size
                        }
                    },
                )
            }
        result.isSuccess shouldBe true
        deferredInRound2 shouldBe 0
        val warning = result.warnings.single().message
        warning shouldContain "ghost"
        warning shouldContain "DeferredLifetimeIntegrationTest"
    }

    case("DEF-04", "最終ラウンドで解決できない deferred が残ったら、対象の一覧付きで ADR どおり警告またはエラーにする", M1) {
        val asError =
            runNative {
                kotlin("d/NeedsNever.kt", "package d\nclass NeedsNever")
                kotlin("d/NeedsMissing.kt", "package d\nclass NeedsMissing")
                native(deferUntilResolvedProvider(mutableListOf()))
            }
        // 既定（unresolvedDeferred = Error）: エラーで、残ったシンボルを全部並べる
        asError.isSuccess shouldBe false
        val message = asError.errors.single().message
        message shouldContain "d.NeedsMissing"
        message shouldContain "d.NeedsNever"

        val asWarning =
            runNative {
                kotlin("d/NeedsNever.kt", "package d\nclass NeedsNever")
                failOnUnresolvedDeferred = false
                native(deferUntilResolvedProvider(mutableListOf()))
            }
        asWarning.isSuccess shouldBe true
        asWarning.warnings.single().message shouldContain "d.NeedsNever"
    }

    case("DEF-05", "ある Processor の deferred が、別の Processor の生成物で解決される", M1) {
        val log = mutableListOf<String>()
        val result =
            runNative {
                kotlin("d/NeedsFromOther.kt", "package d\nclass NeedsFromOther")
                native(deferUntilResolvedProvider(log))
                // 別の Processor が 2 ラウンド目に生成する（それまで defer が続く）
                native(generateAt(1, "Tick"))
                native(generateAt(2, "FromOther"))
            }
        result.isSuccess shouldBe true
        log shouldBe listOf("defer FromOther in round 1", "defer FromOther in round 2", "resolve FromOther in round 3")
    }

    case("LIF-01", "前のラウンドの KaSymbol をラウンド後に使うと、すぐ分かる例外で検出される", M2) {
        var caught: Throwable? = null
        runNative {
            kotlin("d/A.kt", "package d\nclass A")
            native(
                AnalysisCodegenProcessorProvider {
                    object : AnalysisCodegenProcessor {
                        private var kept: KaClassSymbol? = null

                        override fun process(context: AnalysisCodegenRoundContext) {
                            if (context.round == 1) {
                                val cls =
                                    context.allFiles
                                        .single()
                                        .collectDescendantsOfType<KtClass>()
                                        .single()
                                analyze(cls) { kept = cls.symbol as KaClassSymbol }
                                context.codeGenerator.generateKotlin("d", "Tick", Dependencies.all, "package d\nclass Tick")
                            } else {
                                analyze(context.sourceSet.module) { caught = runCatching { kept!!.classId }.exceptionOrNull() }
                            }
                        }
                    }
                },
            )
        }
        caught!!::class.java.name shouldContain "Lifetime"
    }

    case("LIF-02", "analyze スコープの外に持ち出した KaType の利用が検出される", M2) {
        var caught: Throwable? = null
        runNative {
            kotlin("d/A.kt", "package d\nval x: Int = 1")
            native(
                processorProvider { ctx ->
                    if (ctx.round != 1) return@processorProvider
                    val prop =
                        ctx.allFiles
                            .single()
                            .collectDescendantsOfType<org.jetbrains.kotlin.psi.KtProperty>()
                            .single()
                    val escaped: KaType = analyze(prop) { prop.returnType }
                    caught = runCatching { escaped.isMarkedNullable }.exceptionOrNull()
                },
            )
        }
        caught!!::class.java.name shouldContain "Lifetime"
    }

    case("LIF-03", "Processor が持ち続けた古い KtFile / PSI への無効化後のアクセスが検出される", M2) {
        // static に持った KtFile を、ファイルを書き換えた次のビルド（増分）で使う
        val results =
            runCodegenScenario(TestEngine.AnalysisCodegen) {
                initial {
                    kotlin("d/A.kt", "package d\nclass A")
                    native(StaleFileProvider())
                }
                step("A を書き換える") { kotlin("d/A.kt", "package d\nclass A { val changed = 1 }") }
            }
        results[0].result.isSuccess shouldBe true
        StaleFileProvider.secondBuildError!!.message.orEmpty() shouldContain "A.kt"
        (
            StaleFileProvider.secondBuildError!!::class.java.name.contains("Invalid") ||
                StaleFileProvider.secondBuildError!!::class.java.name.contains("Lifetime")
        ) shouldBe
            true
    }
})

/** LIF-03: 1 回目のビルドの KtFile を static に持ち、2 回目のビルドで解析しようとする。 */
class StaleFileProvider : AnalysisCodegenProcessorProvider by processorProvider({ ctx ->
    if (ctx.round != 1) return@processorProvider
    val old = held
    if (old == null) {
        held = ctx.allFiles.single()
    } else {
        secondBuildError = runCatching { analyze(old) { old.declarations.map { (it.symbol as KaClassSymbol).classId } } }.exceptionOrNull()
    }
}) {
    companion object {
        @Volatile var held: KtFile? = null

        @Volatile var secondBuildError: Throwable? = null
    }
}

@file:OptIn(InternalAnalysisCodegenApi::class, ExperimentalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.enginetests.hyb

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Origin
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysis.code.gen.enginetests.support.EngineProject
import me.tbsten.analysis.code.gen.ksp.hybrid.HybridNotSupportedException
import me.tbsten.analysis.code.gen.ksp.hybrid.UnsupportedHybridConversionException
import me.tbsten.analysis.code.gen.ksp.hybrid.isAnalysisCodegenBridge
import me.tbsten.analysis.code.gen.ksp.hybrid.withAnalysis
import me.tbsten.analysis.code.gen.processor.ExperimentalAnalysisCodegenApi
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import me.tbsten.analysis.code.gen.testfoundation.E2ESpec
import me.tbsten.analysis.code.gen.testfoundation.IntegrationSpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import me.tbsten.analysis.code.gen.testing.TestEngine
import me.tbsten.analysis.code.gen.testing.runCodegenTest
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

// 5.19 Hybrid Processor（HYB、ADR-017）。KSP Processor の中から withAnalysis で AA / PSI に触れる

/** HYB-01・02・03 の観察結果。 */
object HybridRecord {
    val lines = mutableListOf<String>()
    var leaked: KaSymbol? = null
    var leakError: Throwable? = null
}

/** `@Hybrid` の関数の本体の呼び出し先（AA で解決）、型の対応、ファイルの対応を記録する。 */
class HybridInspectProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        object : SymbolProcessor {
            override fun process(resolver: Resolver): List<KSAnnotated> {
                HybridRecord.lines += "bridge=${resolver.isAnalysisCodegenBridge}"
                resolver.getSymbolsWithAnnotation("hyb.Hybrid").forEach { node ->
                    when (node) {
                        is KSFunctionDeclaration -> {
                            // HYB-01: 対応する KaSymbol と関数本体（PSI）にスコープ付きでアクセス
                            node.withAnalysis { symbol ->
                                val fn = symbol.psi as KtNamedFunction
                                val callees =
                                    fn.bodyExpression!!.collectDescendantsOfType<KtCallExpression>().mapNotNull { call ->
                                        call.calleeExpression?.text
                                    }
                                HybridRecord.lines += "fn=${fn.name} callees=$callees"
                                HybridRecord.leaked = symbol
                            }
                            // HYB-02: KSType → KaType
                            node.returnType!!.resolve().withAnalysis { type ->
                                HybridRecord.lines +=
                                    "returnType=${type.isMarkedNullable}:$type"
                            }
                            // HYB-02: KSFile → KtFile
                            node.containingFile!!.withAnalysis { ktFile ->
                                HybridRecord.lines +=
                                    "file=${ktFile.name}:${ktFile.declarations.size}"
                            }
                        }
                        is KSClassDeclaration -> {
                            // HYB-02: KSP が合成した宣言（data class の copy・componentN など）は、AA に対応するシンボルがあれば使え、
                            // 無ければ ADR-017 どおり UnsupportedHybridConversionException になる。それ以外の例外は出さない。
                            // 公開 API の withAnalysis は KSDeclaration・KSType・KSFile だけを受けるため、アクセサ（KSPropertyAccessor）は対象外
                            val synthetic =
                                (node.getAllFunctions() + node.getAllProperties()).filter { it.origin == Origin.SYNTHETIC }.toList()
                            HybridRecord.lines += "syntheticCount=${synthetic.size}"
                            synthetic.forEach { decl ->
                                val error = runCatching { decl.withAnalysis { symbol -> symbol.toString() } }.exceptionOrNull()
                                if (error != null && error !is UnsupportedHybridConversionException) {
                                    HybridRecord.lines += "syntheticUnexpected=${decl.simpleName.asString()}:${error::class.simpleName}"
                                }
                            }
                        }
                        else -> {}
                    }
                }
                // HYB-03: スコープの外に持ち出した KaSymbol を使うと検出される
                HybridRecord.leaked?.let { leaked ->
                    HybridRecord.leakError = runCatching { leaked.psi }.exceptionOrNull()
                }
                return emptyList()
            }
        }
}

private fun hybridProject(): EngineProject =
    EngineProject().apply {
        kotlin(
            "hyb/Sources.kt",
            """
            package hyb

            annotation class Hybrid

            fun helper() = 1
            fun other() = 2

            @Hybrid
            fun entry(): Int? = helper() + other()

            @Hybrid
            data class Holder(val value: Int)
            """,
        )
        kspProcessor(HybridInspectProvider::class.java.name)
    }

class HybridProcessorTest : IntegrationSpec({
    case("HYB-01", "KSFunctionDeclaration から対応する KaSymbol と関数本体にスコープ付きでアクセスできる", pending = PendingCase(Milestone.M3)) {
        HybridRecord.lines.clear()
        hybridProject().run().success shouldBe true
        HybridRecord.lines shouldContain "bridge=true"
        HybridRecord.lines shouldContain "fn=entry callees=[helper, other]"
    }

    case("HYB-02", "KSType → KaType、KSFile → KtFile の対応付けが使え、1:1 にならないものは ADR どおり扱う", pending = PendingCase(Milestone.M3)) {
        HybridRecord.lines.clear()
        hybridProject().run().success shouldBe true
        HybridRecord.lines.single { it.startsWith("returnType=") } shouldContain "returnType=true:"
        HybridRecord.lines shouldContain "file=Sources.kt:5"
        HybridRecord.lines.filter { it.startsWith("syntheticUnexpected=") }.shouldBeEmpty()
        HybridRecord.lines shouldNotContain "syntheticCount=0"
    }

    case("HYB-03", "スコープの外に KaSymbol を持ち出すと検出される", pending = PendingCase(Milestone.M3)) {
        HybridRecord.leakError = null
        hybridProject().run()
        val error = checkNotNull(HybridRecord.leakError) { "スコープの外で KaSymbol を使っても例外にならなかった" }
        // AA の有効期間の検査（LIF 系と同じ）で例外になる
        (error.message ?: "") shouldContain "lifetime"
    }
})

/** 素の KSP で withAnalysis を呼ぶ Processor（HYB-04）。 */
class HybridOnPlainKspProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        object : SymbolProcessor {
            override fun process(resolver: Resolver): List<KSAnnotated> {
                plainKspBridgeFlag = resolver.isAnalysisCodegenBridge
                resolver.getSymbolsWithAnnotation("hyb.Hybrid").filterIsInstance<KSFunctionDeclaration>().forEach { fn ->
                    plainKspError = runCatching { fn.withAnalysis { it } }.exceptionOrNull()
                }
                return emptyList()
            }
        }

    companion object {
        var plainKspBridgeFlag: Boolean? = null
        var plainKspError: Throwable? = null
    }
}

class HybridOnPlainKspTest : E2ESpec({
    case("HYB-04", "Hybrid Processor を素の KSP で動かしたときの扱いが ADR どおり（原因が分かるエラーなど）", pending = PendingCase(Milestone.M3)) {
        // ADR-017: 素の KSP では isAnalysisCodegenBridge = false、withAnalysis は HybridNotSupportedException（原因が分かるメッセージ）
        HybridOnPlainKspProvider.plainKspError = null
        val result =
            runCodegenTest(TestEngine.Ksp2) {
                kotlin("hyb/A.kt", "package hyb\n\nannotation class Hybrid\n\n@Hybrid fun entry() = 1")
                ksp(HybridOnPlainKspProvider())
            }
        result.isSuccess shouldBe true
        HybridOnPlainKspProvider.plainKspBridgeFlag shouldBe false
        val error = checkNotNull(HybridOnPlainKspProvider.plainKspError)
        (error is HybridNotSupportedException) shouldBe true
        error.message!! shouldContain "KSP Bridge"
    }
})

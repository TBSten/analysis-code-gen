@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysiscodegen.enginetests.mix

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysiscodegen.engine.Diagnostic
import me.tbsten.analysiscodegen.enginetests.support.EngineProject
import me.tbsten.analysiscodegen.enginetests.support.RecordingEnvironmentFactory
import me.tbsten.analysiscodegen.enginetests.support.engineWith
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessor
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorEnvironment
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorProvider
import me.tbsten.analysiscodegen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysiscodegen.processor.Dependencies
import me.tbsten.analysiscodegen.processor.InternalAnalysisCodegenApi
import me.tbsten.analysiscodegen.testfoundation.IntegrationSpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.psi.KtClass
import com.google.devtools.ksp.processing.Dependencies as KspDependencies

// 5.16 Native と Bridge の相互運用（MIX）の I。Native と KSP が同じラウンド・同じ環境・同じ出力管理を通る（ADR-017）

/** KSP: `@Model` のクラスに `<Name>Mapper` を作る（ラウンド 1）。 */
class KspModelMapperProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        object : SymbolProcessor {
            override fun process(resolver: Resolver): List<KSAnnotated> {
                resolver.getSymbolsWithAnnotation("mix.Model").filterIsInstance<KSClassDeclaration>().forEach { cls ->
                    val name = cls.simpleName.asString()
                    environment.codeGenerator.createNewFile(KspDependencies(false, cls.containingFile!!), "mix", "${name}Mapper").use {
                        it.write("package mix\n\nclass ${name}Mapper(val target: $name)\n".toByteArray())
                    }
                }
                // ラウンド 2 以降: Native が作ったファイルが getNewFiles に現れたら記録する
                resolver.getNewFiles().filter { it.fileName.endsWith("Registry.kt") }.forEach { f ->
                    seenByKsp += f.fileName
                    environment.codeGenerator.createNewFile(KspDependencies(false, f), "mix", "GeneratedFactory").use {
                        it.write("package mix\n\nobject GeneratedFactory { val registry = UserRegistry }\n".toByteArray())
                    }
                }
                return emptyList()
            }
        }

    companion object {
        val seenByKsp = mutableListOf<String>()
    }
}

/** Native: KSP が作った `<Name>Mapper` を解析し、`UserRegistry` を作る（ラウンド 2）。 */
class NativeRegistryProvider : AnalysisCodegenProcessorProvider {
    override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor =
        object : AnalysisCodegenProcessor {
            override fun process(context: AnalysisCodegenRoundContext) {
                context.newFiles.forEach { f ->
                    f.declarations.filterIsInstance<KtClass>().filter { it.name?.endsWith("Mapper") == true }.forEach { mapper ->
                        // KSP の生成物を AA で解析できる（コンストラクタの型が解決できる）
                        val targetType =
                            analyze(context.sourceSet.module) {
                                val symbol = mapper.symbol as KaClassSymbol
                                symbol.memberScope.constructors
                                    .first()
                                    .valueParameters
                                    .first()
                                    .returnType
                                    .toString()
                            }
                        seenByNative += "${mapper.name}:$targetType"
                        context.codeGenerator.generateKotlin(
                            "mix",
                            "UserRegistry",
                            Dependencies.isolating(f),
                            "package mix\n\nobject UserRegistry { val mapper = \"${mapper.name}\" }\n",
                        )
                    }
                }
            }
        }

    companion object {
        val seenByNative = mutableListOf<String>()
    }
}

/** Native と KSP が同じパスに書く（MIX-04）。 */
class NativeCollideProvider : AnalysisCodegenProcessorProvider {
    override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor =
        object : AnalysisCodegenProcessor {
            override fun process(context: AnalysisCodegenRoundContext) {
                if (context.round != 1) return
                context.codeGenerator.generateKotlin("mix", "UserMapper", Dependencies.all, "package mix\n\nclass UserMapper\n")
            }
        }
}

private fun mixProject(vararg native: kotlin.reflect.KClass<out AnalysisCodegenProcessorProvider>): EngineProject =
    EngineProject().apply {
        kotlin("mix/Model.kt", "package mix\n\nannotation class Model\n\n@Model class User(val id: Int)")
        kspProcessor(KspModelMapperProvider::class.java.name)
        native.forEach { nativeProcessor(it) }
    }

class MixedProcessorsTest : IntegrationSpec({
    case("MIX-01", "ラウンド 1 で KSP Processor が生成したコードを、ラウンド 2 で Native Processor が解析できる", pending = PendingCase(Milestone.M1)) {
        NativeRegistryProvider.seenByNative.clear()
        val project = mixProject(NativeRegistryProvider::class)
        project.run().success shouldBe true
        NativeRegistryProvider.seenByNative shouldContain "UserMapper:mix.User"
        project.generated().keys.map { it.substringAfterLast('/') } shouldContain "UserRegistry.kt"
    }

    case("MIX-02", "ラウンド 1 で Native Processor が生成したコードが、ラウンド 2 の KSP の getNewFiles に現れて処理される", pending = PendingCase(Milestone.M1)) {
        KspModelMapperProvider.seenByKsp.clear()
        val project = mixProject(NativeRegistryProvider::class)
        val result = project.run()
        result.success shouldBe true
        // KSP（R1: UserMapper）→ Native（R2: UserRegistry）→ KSP（R3: GeneratedFactory）の 3 ラウンド（設計文書 11.6）
        KspModelMapperProvider.seenByKsp shouldContain "UserRegistry.kt"
        project.generated().keys.map { it.substringAfterLast('/') } shouldContain "GeneratedFactory.kt"
        result.rounds shouldBe 4
    }

    case("MIX-04", "Native と KSP が同じ出力先に書いたらエラー", pending = PendingCase(Milestone.M1)) {
        val project = mixProject(NativeCollideProvider::class)
        val result = project.run()
        result.success shouldBe false
        val error = result.diagnostics.single { it.severity == Diagnostic.Severity.Error }
        error.message shouldContain "UserMapper.kt"
        project.generated() shouldBe emptyMap()
    }

    case("MIX-05", "Bridge と Native が同じ AA 環境を共有する（環境の構築が 1 回）", pending = PendingCase(Milestone.M2)) {
        val project = mixProject(NativeRegistryProvider::class)
        val factory = RecordingEnvironmentFactory()
        project.run(engineWith(factory)).success shouldBe true
        factory.created.get() shouldBe 1
    }
})

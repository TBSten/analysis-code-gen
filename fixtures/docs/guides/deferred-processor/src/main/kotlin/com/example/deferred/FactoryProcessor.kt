package com.example.deferred

import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessor
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorEnvironment
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorProvider
import me.tbsten.analysiscodegen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysiscodegen.processor.Dependencies
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass

private val FACTORY = ClassId.topLevel(FqName("com.example.Factory"))

class FactoryProcessorProvider : AnalysisCodegenProcessorProvider {
    override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor =
        FactoryProcessor()
}

/** `@Factory` の付いたクラスに `create()` を生成する。引数の型が生成待ちなら次のラウンドへ持ち越す。 */
class FactoryProcessor : AnalysisCodegenProcessor {
    override fun process(context: AnalysisCodegenRoundContext) {
        // 前のラウンドで持ち越し、今のラウンドで復元できたもの
        analyze(context.sourceSet.module) {
            for (deferred in context.deferred) {
                val symbol = deferred.pointer.restoreSymbol() as? KaNamedClassSymbol ?: continue
                handle(context, symbol)
            }
        }
        for (file in context.newFiles) {
            analyze(file) {
                file.declarations.filterIsInstance<KtClass>()
                    .mapNotNull { it.classSymbol as? KaNamedClassSymbol }
                    .filter { FACTORY in it.annotations }
                    .forEach { handle(context, it) }
            }
        }
    }

    private fun KaSession.handle(context: AnalysisCodegenRoundContext, symbol: KaNamedClassSymbol) {
        val constructor = symbol.declaredMemberScope.constructors.firstOrNull { it.isPrimary } ?: return
        if (constructor.valueParameters.any { it.returnType is KaErrorType }) {
            // 引数の型がまだ無い（他の Processor が生成する）。KaSymbol ではなく Pointer で持ち越す
            context.defer(symbol.createPointer(), payload = "factory")
            return
        }
        val name = symbol.name.asString()
        val pkg = symbol.classId!!.packageFqName.asString()
        val file = symbol.psi!!.containingFile
        context.codeGenerator.generateKotlin(
            packageName = pkg,
            fileName = "${name}Factory",
            dependencies = Dependencies.isolating(file),
            content = "package $pkg\n\nfun interface ${name}Factory { fun create(): $name }\n",
        )
    }
}

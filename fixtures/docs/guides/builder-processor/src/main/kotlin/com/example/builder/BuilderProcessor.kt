package com.example.builder

import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessor
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorEnvironment
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorProvider
import me.tbsten.analysiscodegen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysiscodegen.processor.Dependencies
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

private val BUILDER = ClassId.topLevel(FqName("com.example.Builder"))

class BuilderProcessorProvider : AnalysisCodegenProcessorProvider {
    override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor =
        BuilderProcessor()
}

class BuilderProcessor : AnalysisCodegenProcessor {
    override fun process(context: AnalysisCodegenRoundContext) {
        // ラウンド 1 は今回処理すべきソース、2 以降は前のラウンドで生成されたファイル
        context.logger.info("round ${context.round}: ${context.newFiles.size} 個を処理します")
        for (file in context.newFiles) {
            analyze(file) {
                file.declarations.filterIsInstance<KtClass>()
                    .filter { it.classSymbol?.annotations?.contains(BUILDER) == true }
                    .forEach { generateBuilder(context, file, it) }
            }
        }
    }

    private fun generateBuilder(context: AnalysisCodegenRoundContext, file: KtFile, klass: KtClass) {
        val name = klass.name!!
        val params = klass.primaryConstructorParameters.map { it.name!! to it.typeReference!!.text }
        val pkg = file.packageFqName.asString()
        val setters = params.joinToString("\n") { (n, t) ->
            "    private var $n: $t? = null\n    fun $n(value: $t) = apply { $n = value }"
        }
        val args = params.joinToString(", ") { (n, _) -> "$n!!" }
        context.codeGenerator.generateKotlin(
            packageName = pkg,
            fileName = "${name}Builder",
            dependencies = Dependencies.isolating(file),
            content = "package $pkg\n\nclass ${name}Builder {\n$setters\n    fun build() = $name($args)\n}\n",
        )
    }
}

package com.example.hybrid

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import me.tbsten.analysiscodegen.processor.ExperimentalAnalysisCodegenApi
import me.tbsten.analysiscodegen.ksp.hybrid.isAnalysisCodegenBridge
import me.tbsten.analysiscodegen.ksp.hybrid.withAnalysis
import org.jetbrains.kotlin.psi.KtNamedFunction

class DescribeProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        DescribeProcessor(environment.codeGenerator)
}

/** `@Describe` の付いたクラスの関数を、本体の式まで含めて一覧にする KSP Processor。 */
@OptIn(ExperimentalAnalysisCodegenApi::class)
class DescribeProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // 素の KSP では Hybrid API が使えないので、関数の本体は「不明」にする
        val canAnalyze = resolver.isAnalysisCodegenBridge
        for (klass in resolver.getSymbolsWithAnnotation("com.example.Describe").filterIsInstance<KSClassDeclaration>()) {
            val lines = klass.getAllFunctions().filter { it.parentDeclaration == klass }.map { function ->
                val body = if (canAnalyze) bodyOf(function) else "不明"
                "${function.simpleName.asString()}: $body"
            }.toList()
            val file = klass.containingFile!!
            // withAnalysis の中で読んだファイルも、KSP の Dependencies に書く
            codeGenerator.createNewFile(Dependencies(aggregating = false, file), "com.example", "${klass.simpleName.asString()}Description").use {
                it.write(lines.joinToString("\n", prefix = "/*\n", postfix = "\n*/\n").toByteArray())
            }
        }
        return emptyList()
    }

    // KSP の宣言から AA のシンボルに入り、関数の本体の式を読む。戻り値は String にして外へ出す
    private fun bodyOf(function: KSFunctionDeclaration): String =
        function.withAnalysis { symbol ->
            (symbol.psi as? KtNamedFunction)?.bodyExpression?.text ?: "なし"
        }
}

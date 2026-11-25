package com.example.hello

import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessor
import me.tbsten.analysiscodegen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysiscodegen.processor.CodegenLogger
import me.tbsten.analysiscodegen.processor.Dependencies
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty

private val HELLO = ClassId.topLevel(FqName("com.example.Hello"))

class HelloProcessor(private val logger: CodegenLogger, private val verbose: Boolean) : AnalysisCodegenProcessor {
    override fun process(context: AnalysisCodegenRoundContext) {
        for (file in context.newFiles) {
            analyze(file) {
                file.declarations.filterIsInstance<KtClassOrObject>()
                    .filter { it.classSymbol?.annotations?.contains(HELLO) == true }
                    .forEach { generateHello(context, file, it.name!!, greeting(it)) }
            }
        }
    }

    private fun KaSession.greeting(cls: KtClassOrObject): String? {
        val property = cls.declarations.filterIsInstance<KtProperty>().firstOrNull { it.name == "greeting" }
        return (property?.initializer?.evaluate() as? KaConstantValue.StringValue)?.value
    }

    private fun generateHello(context: AnalysisCodegenRoundContext, file: KtFile, name: String, greeting: String?) {
        val pkg = file.packageFqName.asString()
        if (verbose) logger.info("generate ${name}Hello")
        context.codeGenerator.generateKotlin(
            packageName = pkg,
            fileName = "${name}Hello",
            dependencies = Dependencies.isolating(file),
            content = "package $pkg\n\nfun $name.hello(): String = \"${greeting ?: "Hello"}, $name!\"\n",
        )
    }
}

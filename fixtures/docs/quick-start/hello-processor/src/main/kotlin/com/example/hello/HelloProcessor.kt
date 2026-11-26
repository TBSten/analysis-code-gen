package com.example.hello

import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessor
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysis.code.gen.processor.Dependencies
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

private val HELLO = ClassId.topLevel(FqName("com.example.Hello"))

class HelloProcessor : AnalysisCodegenProcessor {
    override fun process(context: AnalysisCodegenRoundContext) {
        for (file in context.newFiles) {
            analyze(file) {
                file.declarations.filterIsInstance<KtClassOrObject>()
                    .filter { it.classSymbol?.annotations?.contains(HELLO) == true }
                    .forEach { generateHello(context, file, it.name!!) }
            }
        }
    }
}

private fun generateHello(context: AnalysisCodegenRoundContext, file: KtFile, name: String) {
    val pkg = file.packageFqName.asString()
    context.codeGenerator.generateKotlin(
        packageName = pkg,
        fileName = "${name}Hello",
        dependencies = Dependencies.isolating(file),
        content = "// Auto generated\npackage $pkg\n\nfun $name.hello(): String = \"Hello, $name!\"\n",
    )
}

package com.example.hello

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import me.tbsten.analysis.code.gen.ksp.hybrid.isAnalysisCodegenBridge
import me.tbsten.analysis.code.gen.ksp.hybrid.withAnalysis
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtProperty

class HelloSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val verbose: Boolean,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val bridge = resolver.isAnalysisCodegenBridge
        resolver.getSymbolsWithAnnotation("com.example.Hello")
            .filterIsInstance<KSClassDeclaration>()
            .forEach { generateHello(it, greeting = if (bridge) it.greeting() else null) }
        return emptyList()
    }

    // KSP ではプロパティの初期化式を読めない。AA で定数として評価する
    private fun KSClassDeclaration.greeting(): String? = withAnalysis { symbol ->
        val property = (symbol.psi as? KtClassOrObject)?.declarations
            ?.filterIsInstance<KtProperty>()?.firstOrNull { it.name == "greeting" }
        (property?.initializer?.evaluate() as? KaConstantValue.StringValue)?.value
    }

    private fun generateHello(cls: KSClassDeclaration, greeting: String?) {
        val pkg = cls.packageName.asString()
        val name = cls.simpleName.asString()
        val file = cls.containingFile!!
        if (verbose) logger.info("generate ${name}Hello", cls)
        codeGenerator.createNewFile(Dependencies(false, file), pkg, "${name}Hello").bufferedWriter().use {
            it.write("package $pkg\n\nfun $name.hello(): String = \"${greeting ?: "Hello"}, $name!\"\n")
        }
    }
}

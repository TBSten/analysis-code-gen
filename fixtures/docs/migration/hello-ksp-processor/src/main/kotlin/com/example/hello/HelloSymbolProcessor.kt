package com.example.hello

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class HelloSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val verbose: Boolean,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation("com.example.Hello")
            .filterIsInstance<KSClassDeclaration>()
            .forEach { generateHello(it) }
        return emptyList()
    }

    private fun generateHello(cls: KSClassDeclaration) {
        val pkg = cls.packageName.asString()
        val name = cls.simpleName.asString()
        val file = cls.containingFile!!
        if (verbose) logger.info("generate ${name}Hello", cls)
        codeGenerator.createNewFile(Dependencies(false, file), pkg, "${name}Hello").bufferedWriter().use {
            it.write("package $pkg\n\nfun $name.hello(): String = \"Hello, $name!\"\n")
        }
    }
}

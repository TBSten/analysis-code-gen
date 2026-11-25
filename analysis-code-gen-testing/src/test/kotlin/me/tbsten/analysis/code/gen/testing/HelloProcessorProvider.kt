package me.tbsten.analysis.code.gen.testing

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * テスト用の最小 KSP Processor。`@Hello` の付いたクラスごとに `<名前>Hello` object を生成する。
 * オプション `hello.prefix` で挨拶を変え、`hello.fail=true` でエラーを出す。
 */
class HelloProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        HelloProcessor(environment.codeGenerator, environment.logger, environment.options)
}

private class HelloProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val prefix = options["hello.prefix"] ?: "Hello"
        resolver
            .getSymbolsWithAnnotation("com.example.Hello")
            .filterIsInstance<KSClassDeclaration>()
            .forEach { cls ->
                if (options["hello.fail"] == "true") {
                    logger.error("hello.fail が指定された: ${cls.simpleName.asString()}", cls)
                    return@forEach
                }
                val pkg = cls.packageName.asString()
                val name = "${cls.simpleName.asString()}Hello"
                codeGenerator.createNewFile(Dependencies(false, cls.containingFile!!), pkg, name).bufferedWriter().use {
                    it.write(
                        """
                        |package $pkg
                        |
                        |object $name {
                        |    fun hello(): String = "$prefix, ${cls.simpleName.asString()}"
                        |}
                        |
                        """.trimMargin(),
                    )
                }
            }
        return emptyList()
    }
}

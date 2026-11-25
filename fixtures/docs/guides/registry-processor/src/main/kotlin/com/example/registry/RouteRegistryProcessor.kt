package com.example.registry

import java.io.File
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

private val ROUTE = ClassId.topLevel(FqName("com.example.Route"))

class RouteRegistryProcessorProvider : AnalysisCodegenProcessorProvider {
    override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor =
        RouteRegistryProcessor(environment.options["registry.header"]?.let(::File))
}

/** `@Route` の付いたクラスを集めて、1 つの一覧を生成する。 */
class RouteRegistryProcessor(private val headerFile: File?) : AnalysisCodegenProcessor {
    override fun process(context: AnalysisCodegenRoundContext) {
        // 一覧は全ファイルを見て 1 回だけ作る（ラウンド 2 以降は作り直さない）
        if (context.round != 1) return
        val routes = mutableListOf<Pair<KtFile, String>>()
        for (file in context.allFiles) {
            analyze(file) {
                file.declarations.filterIsInstance<KtClass>()
                    .filter { it.classSymbol?.annotations?.contains(ROUTE) == true }
                    .forEach { routes += file to it.fqName!!.asString() }
            }
        }
        val names = routes.map { it.second }
        writeRegistry(context, routes.map { it.first }.distinct(), names)
        writeServiceFile(context, routes.map { it.first }.distinct(), names)
    }

    private fun writeRegistry(context: AnalysisCodegenRoundContext, files: List<KtFile>, names: List<String>) {
        val header = headerFile?.readText().orEmpty()
        // 一覧は @Route の付いたファイルと、ファイルの増減に依存する。読んだヘッダーのファイルも宣言する
        var dependencies = Dependencies.aggregating(files)
        if (headerFile != null) dependencies = dependencies.withExternalInputs(headerFile)
        context.codeGenerator.generateKotlin(
            packageName = "com.example.generated",
            fileName = "Routes",
            dependencies = dependencies,
            content = "${header}package com.example.generated\n\n" +
                "val routes: List<String> = listOf(${names.joinToString { "\"$it\"" }})\n",
        )
    }

    private fun writeServiceFile(context: AnalysisCodegenRoundContext, files: List<KtFile>, names: List<String>) {
        context.codeGenerator.generateResource(
            relativePath = "META-INF/routes.txt",
            dependencies = Dependencies.aggregating(files),
            content = names.joinToString("\n", postfix = "\n").toByteArray(),
        )
    }
}

package com.example.greeting

import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessor
import me.tbsten.analysiscodegen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysiscodegen.processor.Dependencies
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass

private val GREETING = ClassId.topLevel(FqName("com.example.Greeting"))

class GreetingProcessor(private val prefix: String) : AnalysisCodegenProcessor {
    private var generated = 0

    override fun process(context: AnalysisCodegenRoundContext) {
        for (file in context.newFiles) {
            analyze(file) {
                for (klass in file.declarations.filterIsInstance<KtClass>()) {
                    if (klass.classSymbol?.annotations?.contains(GREETING) != true) continue
                    if (klass.isInterface()) {
                        context.logger.error("@Greeting は interface に付けられません", klass)
                        continue
                    }
                    val name = klass.name!!
                    val pkg = file.packageFqName.asString()
                    context.codeGenerator.generateKotlin(
                        packageName = pkg,
                        fileName = "${name}Greeting",
                        dependencies = Dependencies.isolating(file),
                        content = "package $pkg\n\nfun $name.greet(): String = \"$prefix, $name!\"\n",
                    )
                    generated++
                }
            }
        }
    }

    override fun finish() {
        // 全ラウンドが成功したときだけ呼ばれる。ここでは生成できない
        println("greeting: $generated 個のファイルを生成しました")
    }

    override fun onError() {
        // logger.error か例外があったときに、finish() の代わりに呼ばれる
        generated = 0
    }
}

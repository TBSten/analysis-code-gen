@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysiscodegen.enginetests.support

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessor
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorEnvironment
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorProvider
import me.tbsten.analysiscodegen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysiscodegen.processor.Dependencies
import me.tbsten.analysiscodegen.processor.InternalAnalysisCodegenApi
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import com.google.devtools.ksp.processing.Dependencies as KspDependencies

// ACG-45 の増分 PBT（INC-14・BINC-04・MIX-06）で使う Processor。同じ規則で Native 版と KSP 版を持つ:
// - クラスごとに `<Name>Shape`（isolating）: そのクラスのメンバー名を並べる
// - 全クラスの FQN を並べた `AllShapes`（aggregating）

private fun shapeSource(
    pkg: String,
    name: String,
    members: List<String>,
): String = "package $pkg\n\nobject ${name}Shape { val members = \"${members.sorted().joinToString(",")}\" }\n"

private fun allSource(fqns: List<String>): String = "package shapes\n\nobject AllShapes { val classes = \"${fqns.sorted().joinToString(",")}\" }\n"

/** Native 版。 */
class NativeShapeProvider : AnalysisCodegenProcessorProvider {
    override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor =
        object : AnalysisCodegenProcessor {
            override fun process(context: AnalysisCodegenRoundContext) {
                if (context.round != 1) return
                fun classes(files: List<KtFile>) = files.flatMap { f -> f.declarations.filterIsInstance<KtClassOrObject>().map { f to it } }
                classes(context.newFiles).forEach { (file, cls) ->
                    val pkg = file.packageFqName.asString().ifEmpty { "shapes.root" }
                    context.codeGenerator.generateKotlin(pkg, "${cls.name}Shape", Dependencies.isolating(file), shapeSource(pkg, cls.name!!, cls.declarations.mapNotNull { it.name }))
                }
                val all = classes(context.allFiles)
                context.codeGenerator.generateKotlin("shapes", "AllShapes", Dependencies.aggregating(all.map { it.first }.distinct()), allSource(all.mapNotNull { it.second.fqName?.asString() }))
            }
        }
}

/** KSP 版（Bridge で動かす）。 */
class KspShapeProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        object : SymbolProcessor {
            private var done = false

            override fun process(resolver: Resolver): List<KSAnnotated> {
                if (done) return emptyList()
                done = true
                fun classesOf(files: Sequence<com.google.devtools.ksp.symbol.KSFile>) =
                    files.filter { it.fileName.endsWith(".kt") }.flatMap { f -> f.declarations.filterIsInstance<KSClassDeclaration>().map { f to it } }.toList()
                classesOf(resolver.getNewFiles()).forEach { (file, cls) ->
                    val pkg = file.packageName.asString().ifEmpty { "shapes.root" }
                    val name = cls.simpleName.asString()
                    environment.codeGenerator.createNewFile(KspDependencies(false, file), pkg, "${name}Shape").use {
                        it.write(shapeSource(pkg, name, cls.declarations.map { d -> d.simpleName.asString() }.filter { n -> n != "<init>" }.toList()).toByteArray())
                    }
                }
                val all = classesOf(resolver.getAllFiles())
                environment.codeGenerator.createNewFile(KspDependencies(true, *all.map { it.first }.distinct().toTypedArray()), "shapes", "AllShapes").use {
                    it.write(allSource(all.mapNotNull { it.second.qualifiedName?.asString() }).toByteArray())
                }
                return emptyList()
            }
        }
}

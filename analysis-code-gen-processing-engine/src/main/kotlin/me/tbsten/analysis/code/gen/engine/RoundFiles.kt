@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.engine

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import me.tbsten.analysis.code.gen.analysis.AnalysisEnvironment
import me.tbsten.analysis.code.gen.analysis.ModuleId
import me.tbsten.analysis.code.gen.model.ExecutionUnitModel
import me.tbsten.analysis.code.gen.model.ProcessingScopeModel
import me.tbsten.analysis.code.gen.model.ProjectModel
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

/**
 * 1 ラウンドで Processor に見せるファイル集合（ADR-013 の表）。ラウンドの頭で作り、ラウンド中は同じものを返す（RND-03）。
 *
 * - どのリストも「ソースルートからの相対パス」の辞書順（同じなら rootDir からの相対パス順）。生成物は staging にあるが、
 *   ソースと同じ並びに混ざる（API-07: `A.kt, B.kt, Gen.kt`）
 * - [previousGenerated] はラウンド 2 以降、直前のラウンドでこの実行単位が生成したファイルの rootDir 相対パス。ラウンド 1 は null
 */
internal class RoundFiles(
    val env: AnalysisEnvironment,
    private val model: ProjectModel,
    private val unit: ExecutionUnitModel,
    val owner: String,
    private val roots: List<String>,
    private val previousGenerated: Set<String>?,
) {
    private val rootDir = File(model.rootDir)
    private val kotlinCache = mutableMapOf<String, List<KtFile>>()
    private val javaCache = mutableMapOf<String, List<PsiJavaFile>>()
    private val contexts = mutableMapOf<String, SourceSetContextImpl>()

    fun dependsOn(sourceSet: String): List<String> =
        model.sourceSets
            .firstOrNull { it.name == sourceSet }
            ?.dependsOn
            .orEmpty()

    /** `dependsOn` を遡った上流（近い順、重複なし。自分は含まない）。 */
    private fun ancestors(sourceSet: String): List<String> {
        val result = linkedSetOf<String>()
        val queue = ArrayDeque(dependsOn(sourceSet))
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (next != sourceSet && result.add(next)) queue.addAll(dependsOn(next))
        }
        return result.toList()
    }

    fun ownKotlin(sourceSet: String): List<KtFile> = kotlinCache.getOrPut(sourceSet) { sorted(env.kotlinFiles(ModuleId(sourceSet))) }

    private fun ownJava(sourceSet: String): List<PsiJavaFile> = javaCache.getOrPut(sourceSet) { sorted(env.javaFiles(ModuleId(sourceSet))) }

    fun visibleKotlin(sourceSet: String): List<KtFile> = sorted((listOf(sourceSet) + ancestors(sourceSet)).flatMap(::ownKotlin).distinct())

    private fun visibleJava(sourceSet: String): List<PsiJavaFile> =
        sorted((listOf(sourceSet) + ancestors(sourceSet)).flatMap(::ownJava).distinct())

    fun sourceSetContext(name: String): SourceSetContextImpl = contexts.getOrPut(name) { SourceSetContextImpl(name, this) }

    private val includeDependsOn: Boolean get() = unit.processingScope == ProcessingScopeModel.IncludeDependsOn

    val allKotlin: List<KtFile> by lazy { if (includeDependsOn) visibleKotlin(owner) else ownKotlin(owner) }

    val allJava: List<PsiJavaFile> by lazy { if (includeDependsOn) visibleJava(owner) else ownJava(owner) }

    val newKotlin: List<KtFile> by lazy {
        val previous = previousGenerated ?: return@lazy allKotlin
        allKotlin.filter { rootRelative(it) in previous }
    }

    /** rootDir からの相対パス（rootDir の外ならそのまま）。 */
    fun rootRelative(file: PsiFile): String {
        val raw = (file.virtualFile?.path ?: file.name).replace('\\', '/')
        val f = File(raw)
        return if (f.isAbsolute && f.startsWith(rootDir)) f.relativeTo(rootDir).invariantSeparatorsPath else raw
    }

    private fun <F : PsiFile> sorted(files: List<F>): List<F> {
        val keyed = files.map { file -> Triple(file, sourceRelative(rootRelative(file)), rootRelative(file)) }
        return keyed.sortedWith(compareBy({ it.second }, { it.third })).map { it.first }
    }

    private fun sourceRelative(rootRelativePath: String): String {
        val root = roots.filter { rootRelativePath.startsWith("$it/") }.maxByOrNull { it.length } ?: return rootRelativePath
        return rootRelativePath.removePrefix("$root/")
    }

    companion object {
        /** 並び替えに使うルート（rootDir 相対）。ソースルート・上流の生成物・staging・出力ディレクトリ。 */
        fun rootsOf(
            model: ProjectModel,
            stagingRoots: List<String>,
        ): List<String> {
            val all =
                model.sourceSets.flatMap { it.kotlinSourceRoots + it.javaSourceRoots + it.upstreamGeneratedRoots } +
                    model.units.flatMap { listOf(it.outputDirs.kotlin, it.outputDirs.java) } +
                    stagingRoots
            val rootDir = File(model.rootDir)
            return all
                .map { root ->
                    // モデルのパスは rootDir 相対が原則だが、絶対パスが来ても同じキーにそろえる
                    val f = File(root)
                    if (f.isAbsolute && f.startsWith(rootDir)) f.relativeTo(rootDir).invariantSeparatorsPath else root.replace('\\', '/')
                }.map { it.trimEnd('/') }
                .distinct()
        }
    }
}

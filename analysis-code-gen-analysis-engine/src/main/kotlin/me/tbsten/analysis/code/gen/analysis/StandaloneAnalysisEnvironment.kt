@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.analysis

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import me.tbsten.analysis.code.gen.model.ProjectModel
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.nio.file.Path

/**
 * [AnalysisEnvironment] の実装（ADR-060）。
 *
 * - 追加だけの変更は、セッションを保ったまま content scope と provider に足し、ソースのキャッシュを捨てる（ADR-010、KSP2 と同じ）
 * - 変更・削除を含むとき、または [rebuildOnEveryChange] のときは、セッションを作り直して、これまでに足したファイルを入れ直す。
 *   既存ファイルの PSI を差し替える手段が KSP2 の手順に無いため（エンジンの 1 回の実行では生成物の追加しか起きない）
 */
internal class StandaloneAnalysisEnvironment(
    private val model: ProjectModel,
    private val unitName: String,
    private val rebuildOnEveryChange: Boolean,
) : AnalysisEnvironment {
    private val view = ModelView(model, unitName)
    private val root = File(model.rootDir)

    /** これまでに applyChanges で足したファイル（SourceSet 名 → パス）。作り直すときに入れ直す。 */
    private val addedFiles: Map<String, MutableSet<Path>> = view.sourceSetsInDependencyOrder.associate { it.name to linkedSetOf<Path>() }
    private var session: AaSession = AaSession.create(model, unitName)
    private var closed = false

    override val project: Project get() = checkOpen().project

    override val moduleGraph: AnalysisModuleGraph = ModelModuleGraph(view)

    override fun sourceModule(module: ModuleId): KaSourceModule = checkOpen().sourceModules[module.value] ?: unknown(module)

    override fun libraryModules(module: ModuleId): List<KaLibraryModule> {
        sourceModule(module)
        return checkOpen().libraryModules
    }

    override fun kotlinFiles(module: ModuleId): List<KtFile> {
        val files = checkOpen().kotlinFiles[module.value] ?: unknown(module)
        val upstream = view.upstreamGeneratedRoots(module.value)
        return files
            .filter { file -> upstream.none { Path.of(file.virtualFilePath).startsWith(it) } }
            .sortedBy { it.virtualFilePath }
    }

    override fun javaFiles(module: ModuleId): List<PsiJavaFile> =
        (checkOpen().javaFiles[module.value] ?: unknown(module)).sortedBy { it.virtualFile.path }

    override fun applyChanges(changes: SourceChangeSet): AnalysisInvalidationResult {
        val current = checkOpen()
        val moduleName = changes.module.value
        if (moduleName !in current.sourceModules) unknown(changes.module)
        (changes.added + changes.modified).forEach { (id, content) -> syncToDisk(resolve(id), content) }
        val added = changes.added.keys.map(::resolve)
        val removed = changes.removed.map(::resolve).toSet()
        val known = addedFiles.getValue(moduleName)

        if (rebuildOnEveryChange || changes.modified.isNotEmpty() || changes.removed.isNotEmpty()) {
            known += added
            known -= removed
            rebuild()
        } else {
            val newFiles =
                added.filter {
                    it !in known &&
                        current.kotlinFiles.values
                            .flatten()
                            .none { f -> Path.of(f.virtualFilePath) == it }
                }
            known += newFiles
            current.dropCaches()
            if (newFiles.isNotEmpty()) current.addFiles(moduleName, newFiles)
        }
        return AnalysisInvalidationResult(
            affectedModules = setOf(changes.module) + moduleGraph.dependents(changes.module),
            addedFiles = changes.added.keys,
            removedFiles = changes.removed,
            modifiedFiles = changes.modified.keys,
        )
    }

    override fun close() {
        if (closed) return
        closed = true
        session.close()
    }

    private fun rebuild() {
        session.close()
        // ディスクから消えたファイルは入れ直さない
        addedFiles.values.forEach { files -> files.removeAll { !it.toFile().isFile } }
        session = AaSession.create(model, unitName, addedFiles.mapValues { it.value.toList() })
    }

    private fun resolve(id: SourceFileId): Path = root.resolve(id.path).toPath().normalize()

    private fun syncToDisk(
        path: Path,
        content: String,
    ) {
        val file = path.toFile()
        if (file.isFile && file.readText() == content) return
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    private fun checkOpen(): AaSession {
        check(!closed) { "解析環境（実行単位 $unitName）は閉じている" }
        return session
    }

    private fun unknown(module: ModuleId): Nothing =
        throw IllegalArgumentException("モジュール ${module.value} は実行単位 $unitName の解析環境に無い: ${addedFiles.keys}")
}

/** SourceSet の dependsOn をそのまま写した Module Graph。 */
internal class ModelModuleGraph(
    view: ModelView,
) : AnalysisModuleGraph {
    private val dependsOn = view.sourceSetsInDependencyOrder.associate { it.name to it.dependsOn }
    override val modules: Set<ModuleId> = dependsOn.keys.map(::ModuleId).toSet()

    override fun dependents(module: ModuleId): Set<ModuleId> {
        val result = linkedSetOf<ModuleId>()
        var frontier = setOf(module.value)
        while (frontier.isNotEmpty()) {
            frontier =
                dependsOn
                    .filter { (name, ups) -> ups.any { it in frontier } && ModuleId(name) !in result }
                    .keys
            result += frontier.map(::ModuleId)
        }
        return result - module
    }
}

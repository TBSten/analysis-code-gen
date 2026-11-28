@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.analysis

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import me.tbsten.analysis.code.gen.model.ProjectModel
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.psi.KtFile

/**
 * ラウンドをまたいで維持する AA Standalone の解析環境（設計文書 9.2、ADR-005・010）。
 * 毎ラウンド作り直さない。[applyChanges] で差分だけ反映する。
 */
@InternalAnalysisCodegenApi
public interface AnalysisEnvironment : AutoCloseable {
    public val project: Project
    public val moduleGraph: AnalysisModuleGraph

    /**
     * 差分を反映する。ラウンドの生成物は processing-engine が staging に書いたうえで [SourceChangeSet.added] で渡す。
     * 反映の後に [kotlinFiles] / [javaFiles] を呼び直すと新しいファイルが入る。変わっていないファイルの PSI は作り直さない
     * （前のラウンドの `KaSymbolPointer` を復元できるように）。
     */
    public fun applyChanges(changes: SourceChangeSet): AnalysisInvalidationResult

    /** SourceSet（[ModuleId] は SourceSet 名）に対応する AA のモジュール。 */
    public fun sourceModule(module: ModuleId): KaSourceModule

    /** [module] から見えるライブラリ（クラスパス）のモジュール。 */
    public fun libraryModules(module: ModuleId): List<KaLibraryModule>

    /**
     * [module] の今の Kotlin ファイル。ソースと、[applyChanges] で足した生成物を含む。上流の SourceSet のファイルは含まない。
     * 呼び出し側は結果を持ち続けず、ラウンドごとに呼び直す。
     */
    public fun kotlinFiles(module: ModuleId): List<KtFile>

    /** [module] の今の Java ソース。 */
    public fun javaFiles(module: ModuleId): List<PsiJavaFile>
}

@InternalAnalysisCodegenApi
public interface AnalysisEnvironmentFactory {
    public fun create(
        model: ProjectModel,
        unitName: String,
    ): AnalysisEnvironment
}

/** 実行単位の SourceSet / Compilation を AA のモジュールに写したもの。[ModuleId] は SourceSet 名。 */
@InternalAnalysisCodegenApi
public interface AnalysisModuleGraph {
    public val modules: Set<ModuleId>

    public fun dependents(module: ModuleId): Set<ModuleId>
}

@InternalAnalysisCodegenApi
@JvmInline
public value class ModuleId(public val value: String)

/** rootDir からの相対パスで表したソースファイルの識別子。 */
@InternalAnalysisCodegenApi
@JvmInline
public value class SourceFileId(public val path: String)

@InternalAnalysisCodegenApi
public data class SourceChangeSet(
    val module: ModuleId,
    val added: Map<SourceFileId, String>,
    val modified: Map<SourceFileId, String>,
    val removed: Set<SourceFileId>,
)

@InternalAnalysisCodegenApi
public data class AnalysisInvalidationResult(
    val affectedModules: Set<ModuleId>,
    val addedFiles: Set<SourceFileId>,
    val removedFiles: Set<SourceFileId>,
    val modifiedFiles: Set<SourceFileId>,
)

/** 本番の実装。 */
@InternalAnalysisCodegenApi
public class StandaloneAnalysisEnvironmentFactory : AnalysisEnvironmentFactory {
    override fun create(
        model: ProjectModel,
        unitName: String,
    ): AnalysisEnvironment = TODO()
}

/** 毎回環境を作り直す比較用の実装（CCH-05）。 */
@InternalAnalysisCodegenApi
public class ReferenceAnalysisEnvironmentFactory : AnalysisEnvironmentFactory {
    override fun create(
        model: ProjectModel,
        unitName: String,
    ): AnalysisEnvironment = TODO()
}

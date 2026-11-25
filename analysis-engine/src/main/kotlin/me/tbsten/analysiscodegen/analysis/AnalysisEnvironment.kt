@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysiscodegen.analysis

import com.intellij.openapi.project.Project
import me.tbsten.analysiscodegen.model.ProjectModel
import me.tbsten.analysiscodegen.processor.InternalAnalysisCodegenApi

/**
 * ラウンドをまたいで維持する AA Standalone の解析環境（設計文書 9.2、ADR-005・010）。
 * 毎ラウンド作り直さない。[applyChanges] で差分だけ反映する。
 */
@InternalAnalysisCodegenApi
public interface AnalysisEnvironment : AutoCloseable {
    public val project: Project
    public val moduleGraph: AnalysisModuleGraph

    public fun applyChanges(changes: SourceChangeSet): AnalysisInvalidationResult
}

@InternalAnalysisCodegenApi
public interface AnalysisEnvironmentFactory {
    public fun create(model: ProjectModel, unitName: String): AnalysisEnvironment
}

/** 実行単位の SourceSet / Compilation を AA のモジュールに写したもの。 */
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
    override fun create(model: ProjectModel, unitName: String): AnalysisEnvironment = TODO()
}

/** 毎回環境を作り直す比較用の実装（CCH-05）。 */
@InternalAnalysisCodegenApi
public class ReferenceAnalysisEnvironmentFactory : AnalysisEnvironmentFactory {
    override fun create(model: ProjectModel, unitName: String): AnalysisEnvironment = TODO()
}

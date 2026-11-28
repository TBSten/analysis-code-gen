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
 * ラウンドをまたいで維持する AA Standalone の解析環境（設計文書 9.2、ADR-005・010・060）。
 * 毎ラウンド作り直さない。[applyChanges] で差分だけ反映する。
 *
 * - モジュールは SourceSet 1 つにつき 1 つ（[ModuleId] は SourceSet 名、ADR-009）
 * - [applyChanges] の後は [kotlinFiles] / [javaFiles] を呼び直す。変わっていないファイルの PSI は同じものが返る
 * - 実行単位の終わりに [close] する。閉じると [project] は dispose され、PSI と KaSymbol は使えなくなる
 */
@InternalAnalysisCodegenApi
public interface AnalysisEnvironment : AutoCloseable {
    public val project: Project
    public val moduleGraph: AnalysisModuleGraph

    /** [module] の SourceSet の KaSourceModule。`analyze(module) { }` に渡す。 */
    public fun sourceModule(module: ModuleId): KaSourceModule

    /** [module] が依存するライブラリ（classpath・friend・JDK）。 */
    public fun libraryModules(module: ModuleId): List<KaLibraryModule>

    /** [module] の現在の Kotlin ファイル（[applyChanges] で足したものを含み、上流の生成物 upstreamGeneratedRoots は含まない）。パス順。 */
    public fun kotlinFiles(module: ModuleId): List<KtFile>

    /** [module] の現在の Java ファイル。JVM 以外の Compilation では空。パス順。 */
    public fun javaFiles(module: ModuleId): List<PsiJavaFile>

    /**
     * 差分を反映する。ラウンドの生成物は processing-engine が staging に書いたうえで [SourceChangeSet.added] で渡す。
     * 反映の後に [kotlinFiles] / [javaFiles] を呼び直すと新しいファイルが入る。変わっていないファイルの PSI は作り直さない
     * （前のラウンドの `KaSymbolPointer` を復元できるように）。変更・削除を含むときは環境を作り直すので PSI も新しくなる（ADR-060）。
     */
    public fun applyChanges(changes: SourceChangeSet): AnalysisInvalidationResult
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

    /** [module] に dependsOn で（直接・間接に）依存するモジュール。[module] 自身は含まない。 */
    public fun dependents(module: ModuleId): Set<ModuleId>
}

/** SourceSet 名（例: `jvmMain`）。 */
@InternalAnalysisCodegenApi
@JvmInline
public value class ModuleId(public val value: String)

/** rootDir からの相対パスで表したソースファイルの識別子。 */
@InternalAnalysisCodegenApi
@JvmInline
public value class SourceFileId(public val path: String)

/**
 * 1 モジュールへの変更。値はファイルの中身。
 * ディスクの内容と違う（または無い）ときは、反映の前に中身をディスクへ書く（ADR-060）。
 */
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

/** 本番の実装。ラウンドの間は環境を保ち、生成物を差分で足す（ADR-010）。 */
@InternalAnalysisCodegenApi
public class StandaloneAnalysisEnvironmentFactory : AnalysisEnvironmentFactory {
    override fun create(
        model: ProjectModel,
        unitName: String,
    ): AnalysisEnvironment = StandaloneAnalysisEnvironment(model, unitName, rebuildOnEveryChange = false)
}

/** 毎回環境を作り直す比較用の実装（CCH-05）。変更のたびに環境を作り直し、これまでに足したファイルを入れ直す。 */
@InternalAnalysisCodegenApi
public class ReferenceAnalysisEnvironmentFactory : AnalysisEnvironmentFactory {
    override fun create(
        model: ProjectModel,
        unitName: String,
    ): AnalysisEnvironment = StandaloneAnalysisEnvironment(model, unitName, rebuildOnEveryChange = true)
}

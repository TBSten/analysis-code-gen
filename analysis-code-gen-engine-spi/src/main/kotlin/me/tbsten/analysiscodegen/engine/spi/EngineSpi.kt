@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysiscodegen.engine.spi

import com.intellij.openapi.project.Project
import me.tbsten.analysiscodegen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysiscodegen.processor.Dependencies
import me.tbsten.analysiscodegen.processor.ExecutionUnit
import me.tbsten.analysiscodegen.processor.InternalAnalysisCodegenApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.psi.KtElement
import java.io.File

/**
 * 処理エンジンに Processor の種類を足す口（ADR-017）。ServiceLoader で探す。
 * Native 用はエンジンに内蔵、KSP 用は analysis-code-gen-ksp-bridge が提供する。
 */
@InternalAnalysisCodegenApi
public interface ProcessorFrontend {
    /** 例: `native`、`ksp`。 */
    public val id: String

    /** [component] にこの Frontend が扱える Provider があるか。 */
    public fun canHandle(component: ProcessorComponent): Boolean

    /** [component] の Provider から、この実行単位の Processor を作る。 */
    public fun create(
        component: ProcessorComponent,
        environment: EngineProcessorEnvironment,
    ): List<EngineProcessor>
}

/** Processor の依存 1 件（Gradle の component 1 つ）。 */
@InternalAnalysisCodegenApi
public interface ProcessorComponent {
    public val coordinates: String
    public val classLoader: ClassLoader

    /** `META-INF/services/<serviceName>` を持つか。 */
    public fun hasService(serviceName: String): Boolean
}

@InternalAnalysisCodegenApi
public interface EngineProcessorEnvironment {
    public val executionUnit: ExecutionUnit
    public val options: Map<String, String>
    public val kotlinVersion: String
    public val frameworkVersion: String
    public val logger: EngineLogger
    public val analysis: AnalysisAccess
}

/** エンジンが実際に回す Processor。Native もエンジン内部ではこれとして扱う。 */
@InternalAnalysisCodegenApi
public interface EngineProcessor {
    /** 診断に出す名前。 */
    public val name: String

    public fun process(context: EngineRoundContext)

    public fun finish(context: EngineFinishContext)

    public fun onError()

    /** 増分処理に参加するなら返す。 */
    public val incremental: IncrementalParticipant? get() = null
}

@InternalAnalysisCodegenApi
public interface EngineRoundContext {
    /** Native と同じ見え方の RoundContext。 */
    public val native: AnalysisCodegenRoundContext
    public val output: OutputSink
    public val analysis: AnalysisAccess
}

@InternalAnalysisCodegenApi
public interface EngineFinishContext {
    /** finish 中の生成を許す Processor（KSP 互換）だけが使う。生成物は次のラウンドの入力にならない。 */
    public val output: OutputSink
}

@InternalAnalysisCodegenApi
public enum class OutputKind { Kotlin, Java, Resource }

/** 同じ Processor が同じパスに 2 回書いたときの扱い（ADR-014・017）。 */
@InternalAnalysisCodegenApi
public enum class DuplicatePolicy {
    /** Native の既定。同じラウンドならエラー、後のラウンドで同じ内容なら無視。 */
    Native,

    /** KSP と同じ `FileAlreadyExistsException` を投げる。 */
    ThrowKspException,
}

@InternalAnalysisCodegenApi
public interface OutputSink {
    public fun write(
        kind: OutputKind,
        relativePath: String,
        content: ByteArray,
        dependencies: Dependencies,
        duplicatePolicy: DuplicatePolicy,
    )

    /** 既存の出力に依存を足す（KSP の `associate*`）。 */
    public fun associate(
        kind: OutputKind,
        relativePath: String,
        dependencies: Dependencies,
    )
}

@InternalAnalysisCodegenApi
public interface EngineLogger {
    public fun log(
        severity: Severity,
        message: String,
        element: KtElement? = null,
    )

    public enum class Severity { Logging, Info, Warn, Error }
}

/** Bridge など移植コードが AA に触れる口。足すときは ADR-017 に追記する。 */
@InternalAnalysisCodegenApi
public interface AnalysisAccess {
    public val project: Project
    public val sourceModule: KaSourceModule
    public val libraryModules: List<KaLibraryModule>

    public fun <R> analyze(
        element: KtElement,
        block: KaSession.() -> R,
    ): R
}

/** 増分処理への参加口（KSP の Lookup 追跡の移植先）。 */
@InternalAnalysisCodegenApi
public interface IncrementalParticipant {
    /** 変わった・消えたファイルから、追加で dirty にすべきソースを返す。全件処理が必要なら null。 */
    public fun expandDirtyFiles(
        changed: Set<String>,
        removed: Set<String>,
    ): Set<String>?

    public fun load(stateDir: File)

    public fun save(stateDir: File)
}

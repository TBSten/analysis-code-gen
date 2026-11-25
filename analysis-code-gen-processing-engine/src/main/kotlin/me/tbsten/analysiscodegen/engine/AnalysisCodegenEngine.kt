@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysiscodegen.engine

import me.tbsten.analysiscodegen.analysis.AnalysisEnvironmentFactory
import me.tbsten.analysiscodegen.engine.spi.EngineProcessor
import me.tbsten.analysiscodegen.engine.spi.EngineProcessorEnvironment
import me.tbsten.analysiscodegen.engine.spi.ProcessorComponent
import me.tbsten.analysiscodegen.engine.spi.ProcessorFrontend
import me.tbsten.analysiscodegen.incremental.DirtyFileCalculator
import me.tbsten.analysiscodegen.model.ProjectModel
import me.tbsten.analysiscodegen.processor.InternalAnalysisCodegenApi

/**
 * 1 実行単位を処理するエンジンの入口。Gradle の Worker・CLI・testing から呼ぶ。
 */
@InternalAnalysisCodegenApi
public class AnalysisCodegenEngine(
    private val components: EngineComponents = EngineComponents.default(),
) {
    public fun run(request: EngineRequest): EngineResult = TODO()
}

@InternalAnalysisCodegenApi
public data class EngineRequest(
    val model: ProjectModel,
    val unitName: String,
    /** 増分を使わず全件処理する（`--rerun-tasks` 相当）。 */
    val forceFull: Boolean = false,
)

@InternalAnalysisCodegenApi
public data class EngineResult(
    val success: Boolean,
    val rounds: Int,
    val diagnostics: List<Diagnostic>,
    val generatedFiles: List<String>,
)

@InternalAnalysisCodegenApi
public data class Diagnostic(
    val severity: Severity,
    val message: String,
    val processorName: String?,
    /** rootDir からの相対パス。 */
    val file: String?,
    val line: Int?,
) {
    public enum class Severity { Info, Warning, Error }
}

/**
 * エンジンの部品の組み立て。DI フレームワークは使わず、ここに集める（ADR-012）。
 */
@InternalAnalysisCodegenApi
public class EngineComponents(
    public val analysisEnvironmentFactory: AnalysisEnvironmentFactory,
    public val dirtyFileCalculator: DirtyFileCalculator,
    public val frontends: List<ProcessorFrontend>,
) {
    public companion object {
        /** Native の Frontend と、ServiceLoader で見つけた Frontend（Bridge など）で組み立てる。 */
        public fun default(): EngineComponents = TODO()
    }
}

/** Native Processor を [EngineProcessor] として回す（ADR-017）。 */
internal class NativeProcessorFrontend : ProcessorFrontend {
    override val id: String = "native"

    override fun canHandle(component: ProcessorComponent): Boolean = TODO()

    override fun create(
        component: ProcessorComponent,
        environment: EngineProcessorEnvironment,
    ): List<EngineProcessor> = TODO()
}

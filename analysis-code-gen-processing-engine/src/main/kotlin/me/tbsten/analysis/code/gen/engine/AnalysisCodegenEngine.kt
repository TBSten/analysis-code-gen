@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.engine

import me.tbsten.analysis.code.gen.analysis.AnalysisEnvironmentFactory
import me.tbsten.analysis.code.gen.analysis.StandaloneAnalysisEnvironmentFactory
import me.tbsten.analysis.code.gen.engine.spi.ProcessorComponent
import me.tbsten.analysis.code.gen.engine.spi.ProcessorFrontend
import me.tbsten.analysis.code.gen.incremental.DirtyFileCalculator
import me.tbsten.analysis.code.gen.model.ProjectModel
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi

/**
 * 1 実行単位を処理するエンジンの入口。Gradle の Worker・CLI・testing から呼ぶ。
 *
 * 1 回の [run] で、Processor の作成 → ラウンドの繰り返し → finish → Build Commit までを行う（ADR-013・014）。
 */
@InternalAnalysisCodegenApi
public class AnalysisCodegenEngine(
    private val components: EngineComponents = EngineComponents.default(),
) {
    public fun run(request: EngineRequest): EngineResult {
        val unit =
            requireNotNull(request.model.units.firstOrNull { it.name == request.unitName }) {
                "実行単位 ${request.unitName} がモデルに無い。ある単位: ${request.model.units.map { it.name }}"
            }
        return UnitRun(components, request.model, unit, request.extraComponents).run()
    }
}

@InternalAnalysisCodegenApi
public data class EngineRequest(
    val model: ProjectModel,
    val unitName: String,
    /** 増分を使わず全件処理する（`--rerun-tasks` 相当）。 */
    val forceFull: Boolean = false,
    /**
     * モデルの `processors` の後ろに足す Processor の component。testing が Provider のインスタンスを直接渡すときに使う
     * （[InstanceProcessorComponent]、ADR-061）。
     */
    val extraComponents: List<ProcessorComponent> = emptyList(),
)

@InternalAnalysisCodegenApi
public data class EngineResult(
    val success: Boolean,
    val rounds: Int,
    val diagnostics: List<Diagnostic>,
    /** 出力ディレクトリに置いた生成物の、rootDir からの相対パス（辞書順）。失敗したときは空。 */
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

    /** ADR-040 の 1 行の書式（`<severity>: [<processor>] <file>:<line>: <message>`。単位名は呼び出し側が足す）。 */
    public fun render(): String =
        buildString {
            append(severity.name.lowercase())
            append(": ")
            processorName?.let { append("[").append(it).append("] ") }
            if (file != null) {
                append(file)
                line?.let { append(":").append(it) }
                append(": ")
            }
            append(message)
        }
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
        public fun default(): EngineComponents =
            EngineComponents(
                analysisEnvironmentFactory = StandaloneAnalysisEnvironmentFactory(),
                dirtyFileCalculator = DirtyFileCalculator(),
                frontends = defaultFrontends(),
            )

        /** Native を先頭に置く。同じ component を扱える Frontend が複数あれば先頭を選ぶ（ADR-017）。 */
        public fun defaultFrontends(): List<ProcessorFrontend> {
            val discovered =
                java.util.ServiceLoader
                    .load(ProcessorFrontend::class.java, EngineComponents::class.java.classLoader)
                    .toList()
                    .filter { it.id != NativeProcessorFrontend.ID }
            return listOf(NativeProcessorFrontend()) + discovered
        }
    }
}

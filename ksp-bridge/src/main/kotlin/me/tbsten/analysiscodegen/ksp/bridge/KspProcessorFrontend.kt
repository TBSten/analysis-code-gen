@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysiscodegen.ksp.bridge

import me.tbsten.analysiscodegen.engine.spi.EngineProcessor
import me.tbsten.analysiscodegen.engine.spi.EngineProcessorEnvironment
import me.tbsten.analysiscodegen.engine.spi.ProcessorComponent
import me.tbsten.analysiscodegen.engine.spi.ProcessorFrontend
import me.tbsten.analysiscodegen.processor.InternalAnalysisCodegenApi

/**
 * KSP の `SymbolProcessorProvider` をエンジンに参加させる（ADR-017）。
 * `META-INF/services/me.tbsten.analysiscodegen.engine.spi.ProcessorFrontend` で登録する。
 *
 * 同じ component に Native の Provider があれば、エンジンは Native を選ぶ（API-14）。
 */
public class KspProcessorFrontend : ProcessorFrontend {
    override val id: String = "ksp"

    override fun canHandle(component: ProcessorComponent): Boolean = TODO()

    override fun create(
        component: ProcessorComponent,
        environment: EngineProcessorEnvironment,
    ): List<EngineProcessor> = TODO()

    public companion object {
        public const val KSP_PROVIDER_SERVICE: String =
            "com.google.devtools.ksp.processing.SymbolProcessorProvider"
    }
}

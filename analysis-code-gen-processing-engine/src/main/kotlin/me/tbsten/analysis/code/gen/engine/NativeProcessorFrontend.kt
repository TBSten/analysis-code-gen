@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.engine

import me.tbsten.analysis.code.gen.engine.spi.EngineFinishContext
import me.tbsten.analysis.code.gen.engine.spi.EngineProcessor
import me.tbsten.analysis.code.gen.engine.spi.EngineProcessorEnvironment
import me.tbsten.analysis.code.gen.engine.spi.EngineRoundContext
import me.tbsten.analysis.code.gen.engine.spi.ProcessorComponent
import me.tbsten.analysis.code.gen.engine.spi.ProcessorFrontend
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessor
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorEnvironment
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorProvider
import me.tbsten.analysis.code.gen.processor.CodegenLogger
import me.tbsten.analysis.code.gen.processor.ExecutionUnit
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi

/** Native Processor を [EngineProcessor] として回す（ADR-017）。 */
internal class NativeProcessorFrontend : ProcessorFrontend {
    override val id: String = ID

    override fun canHandle(component: ProcessorComponent): Boolean = component.hasService(SERVICE)

    override fun create(
        component: ProcessorComponent,
        environment: EngineProcessorEnvironment,
    ): List<EngineProcessor> =
        component.loadServices(AnalysisCodegenProcessorProvider::class.java).map { provider ->
            // 診断に出す名前は Provider のクラス名（利用者が META-INF/services に書いた名前と同じ）
            val name = provider.javaClass.name
            val logger =
                (environment as? EngineProcessorEnvironmentImpl)?.collector?.let { CodegenLoggerImpl(it, name) }
                    ?: EngineLoggerAsCodegenLogger(environment.logger)
            val nativeEnvironment = NativeEnvironment(environment, logger)
            val processor =
                try {
                    provider.create(nativeEnvironment)
                } catch (t: Throwable) {
                    throw ProcessorFailure(name, "Provider の create", t)
                }
            NativeProcessorAdapter(name, processor)
        }

    companion object {
        const val ID: String = "native"
        val SERVICE: String = AnalysisCodegenProcessorProvider::class.java.name
    }
}

/** Provider・Processor が投げた例外を、どの Processor のどの段階かと一緒に運ぶ。 */
internal class ProcessorFailure(
    val processorName: String,
    val phase: String,
    cause: Throwable,
) : RuntimeException("$processorName: $phase", cause)

private class NativeEnvironment(
    private val engine: EngineProcessorEnvironment,
    override val logger: CodegenLogger,
) : AnalysisCodegenProcessorEnvironment {
    override val options: Map<String, String> get() = engine.options
    override val executionUnit: ExecutionUnit get() = engine.executionUnit
    override val kotlinVersion: String get() = engine.kotlinVersion
    override val frameworkVersion: String get() = engine.frameworkVersion
}

/**
 * Native の Processor。生成・ログは [EngineRoundContext.native] が持つ（Processor ごとに作られ、ラウンドが終わると閉じる）。
 * finish / onError の中で生成しようとすると、閉じた RoundContext が [IllegalStateException] を投げる（ADR-013）。
 */
internal class NativeProcessorAdapter(
    override val name: String,
    private val delegate: AnalysisCodegenProcessor,
) : EngineProcessor {
    override fun process(context: EngineRoundContext) = delegate.process(context.native)

    override fun finish(context: EngineFinishContext) = delegate.finish()

    override fun onError() = delegate.onError()
}

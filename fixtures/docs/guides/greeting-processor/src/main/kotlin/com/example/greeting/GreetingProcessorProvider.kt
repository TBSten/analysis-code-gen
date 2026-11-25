package com.example.greeting

import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessor
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorEnvironment
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorProvider

class GreetingProcessorProvider : AnalysisCodegenProcessorProvider {
    override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor {
        val prefix = environment.options["greeting.prefix"] ?: "Hello"
        environment.logger.info("greeting: ${environment.executionUnit.name} で動きます")
        return GreetingProcessor(prefix)
    }
}

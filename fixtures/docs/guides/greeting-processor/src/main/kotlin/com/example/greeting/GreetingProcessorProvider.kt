package com.example.greeting

import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessor
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorEnvironment
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorProvider

class GreetingProcessorProvider : AnalysisCodegenProcessorProvider {
    override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor {
        val prefix = environment.options["greeting.prefix"] ?: "Hello"
        environment.logger.info("greeting: ${environment.executionUnit.name} で動きます")
        return GreetingProcessor(prefix)
    }
}

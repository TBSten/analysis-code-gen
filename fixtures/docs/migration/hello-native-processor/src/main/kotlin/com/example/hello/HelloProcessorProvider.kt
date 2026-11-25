package com.example.hello

import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessor
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorEnvironment
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorProvider

class HelloProcessorProvider : AnalysisCodegenProcessorProvider {
    override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor =
        HelloProcessor(
            logger = environment.logger,
            verbose = environment.options["hello.verbose"] == "true",
        )
}

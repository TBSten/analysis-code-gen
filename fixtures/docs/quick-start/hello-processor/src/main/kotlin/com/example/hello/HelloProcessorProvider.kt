package com.example.hello

import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessor
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorEnvironment
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorProvider

class HelloProcessorProvider : AnalysisCodegenProcessorProvider {
    override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor =
        HelloProcessor()
}

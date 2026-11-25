package com.example.hello

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class HelloSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        HelloSymbolProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            verbose = environment.options["hello.verbose"] == "true",
        )
}

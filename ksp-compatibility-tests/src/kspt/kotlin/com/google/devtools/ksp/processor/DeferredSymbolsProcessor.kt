/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/DeferredSymbolsProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated

class DeferredSymbolsProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> = result.sorted()

    var round = 0

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()

        if (round++ == 0) {
            env.codeGenerator.createNewFile(Dependencies(false), "", "Unused", "kt").use {
                it.write("class Unused".toByteArray())
            }

            deferred.addAll(resolver.getSymbolsWithAnnotation("Defer"))
        } else {
            val symbols = resolver.getSymbolsWithAnnotation("Defer")
            result.addAll(symbols.map(Any::toString).toList())
        }

        return deferred
    }

    lateinit var env: SymbolProcessorEnvironment

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        env = environment
        return super.create(environment)
    }
}

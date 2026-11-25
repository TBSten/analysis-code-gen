/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/GetSymbolsFromAnnotationProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class GetSymbolsFromAnnotationProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    val result = mutableListOf<List<String>>()

    override fun toResult(): List<String> = result.flatten()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        result.add("==== Anno inDepth = false ====")
        resolver.getSymbolsWithAnnotation("Anno", inDepth = false).prepare().let { result.add(it) }
        result.add("==== Anno inDepth = true ====")
        resolver.getSymbolsWithAnnotation("Anno", inDepth = true).prepare().let { result.add(it) }
        result.add("==== Bnno inDepth = false ====")
        resolver.getSymbolsWithAnnotation("Bnno", inDepth = false).prepare().let { result.add(it) }
        result.add("==== Bnno inDepth = true ====")
        resolver.getSymbolsWithAnnotation("Bnno", inDepth = true).prepare().let { result.add(it) }
        result.add("==== A1 inDepth = false ====")
        resolver.getSymbolsWithAnnotation("A1", inDepth = false).prepare().let { result.add(it) }
        result.add("==== A1 inDepth = true ====")
        resolver.getSymbolsWithAnnotation("A1", inDepth = true).prepare().let { result.add(it) }
        result.add("==== A2 inDepth = false ====")
        resolver.getSymbolsWithAnnotation("A2", inDepth = false).prepare().let { result.add(it) }
        result.add("==== A2 inDepth = true ====")
        resolver.getSymbolsWithAnnotation("A2", inDepth = true).prepare().let { result.add(it) }
        result.add("==== Cnno inDepth = true ====")
        resolver.getSymbolsWithAnnotation("Cnno", inDepth = true).prepare().let { result.add(it) }
        result.add("==== MyNestedAnnotation inDepth = false ====")
        resolver
            .getSymbolsWithAnnotation("AnnotationContainer1.MyNestedAnnotation", inDepth = false)
            .prepare {
                toString(it) + ", " +
                    it.annotations.joinToString(", ") { anno ->
                        anno.annotationType
                            .resolve()
                            .declaration.qualifiedName
                            ?.asString() ?: "ERROR"
                    }
            }.let { result.add(it) }
        resolver
            .getSymbolsWithAnnotation("AnnotationContainer2.MyNestedAnnotation", inDepth = false)
            .prepare {
                toString(it) + ", " +
                    it.annotations.joinToString(", ") { anno ->
                        anno.annotationType
                            .resolve()
                            .declaration.qualifiedName
                            ?.asString() ?: "ERROR"
                    }
            }.let { result.add(it) }
        return emptyList()
    }

    private fun Sequence<KSAnnotated>.prepare(transform: (KSAnnotated) -> String = { annotated -> toString(annotated) }): List<String> =
        toList().map { transform(it) }.sorted()

    private fun <A> MutableList<List<A>>.add(el: A) = add(listOf(el))

    private fun toString(annotated: KSAnnotated): String =
        "$annotated:${annotated::class.supertypes.first().classifier.toString().substringAfterLast('.')}"
}

/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/PackageProviderForGeneratedProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated

class PackageProviderForGeneratedProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> = result

    var round = 0

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (round == 0) {
            createJavaFile("foo.bar", "MyGeneratedJavaClass")
        }
        result.add("Processing round $round")
        val getByName =
            resolver
                .getClassDeclarationByName(
                    resolver.getKSNameFromString("foo.bar.MyGeneratedJavaClass"),
                )?.qualifiedName
                ?.asString()
        result.add("resolver.getClassDeclarationByName: $getByName")
        val getByPackage = resolver.getDeclarationsFromPackage("foo.bar").toList().map { it.qualifiedName?.asString() }
        result.add("resolver.getDeclarationsFromPackage: $getByPackage")
        round++
        return emptyList()
    }

    lateinit var env: SymbolProcessorEnvironment

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        env = environment
        return super.create(environment)
    }

    private fun createJavaFile(
        packageName: String,
        className: String,
    ) {
        val dependencies = Dependencies(aggregating = false, sources = arrayOf())
        env.codeGenerator.createNewFile(dependencies, packageName, className, "java").use {
            it.write("package foo.bar;\nclass MyGeneratedJavaClass{}".toByteArray())
        }
    }
}

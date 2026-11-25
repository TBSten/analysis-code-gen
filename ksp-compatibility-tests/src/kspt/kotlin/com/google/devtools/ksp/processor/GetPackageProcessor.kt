/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/GetPackageProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class GetPackageProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> = results

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        addPackage("lib1", resolver)
        addPackage("lib2", resolver)
        addPackage("main.test", resolver)
        addPackage("non.exist", resolver)
        addPackage("test", resolver)
        addPackage("testlib", resolver)
        addPackage("main.nested", resolver)
        return emptyList()
    }

    @KspExperimental
    private fun addPackage(
        name: String,
        resolver: Resolver,
    ) {
        results.add("symbols from package $name")
        resolver.getDeclarationsFromPackage(name).forEach {
            results.add("${it.qualifiedName?.asString() ?: "error"} ${it.origin}")
        }
    }
}

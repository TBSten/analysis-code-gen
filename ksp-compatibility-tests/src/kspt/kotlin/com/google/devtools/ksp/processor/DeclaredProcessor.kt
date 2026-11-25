/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/DeclaredProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class DeclaredProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> = result

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val sub = resolver.getClassDeclarationByName("Sub")!!
        val base = resolver.getClassDeclarationByName("Base")!!
        val javasource = resolver.getClassDeclarationByName("JavaSource")!!
        result.add("Base class declared functions:")
        sub.declarations.forEach { result.add(it.toString()) }
        result.add("Sub class declared functions:")
        base.declarations.forEach { result.add(it.toString()) }
        result.add("JavaSource class declared functions:")
        javasource.declarations.forEach { result.add(it.toString()) }
        return emptyList()
    }
}

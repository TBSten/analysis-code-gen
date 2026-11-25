/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/test/kotlin/com/google/devtools/ksp/processor/DefaultKClassValueProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.impl.symbol.kotlin.KSTypeImpl
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class DefaultKClassValueProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> = results

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val example1 = resolver.getClassDeclarationByName("Example")!!.annotations.first()
        val example2 = resolver.getClassDeclarationByName("Example2")!!.annotations.first()
        val arg1 = (example1.arguments.single().value as KSTypeImpl).declaration.qualifiedName!!
        val defaultArg1 = (example1.defaultArguments.single().value as KSTypeImpl).declaration.qualifiedName!!
        results.add(defaultArg1.asString())
        results.add(arg1.asString())
        val arg2 = (example2.arguments.single().value as KSTypeImpl).declaration.qualifiedName!!
        val defaultArg2 = (example2.defaultArguments.single().value as KSTypeImpl).declaration.qualifiedName!!
        results.add(defaultArg2.asString())
        results.add(arg2.asString())
        return emptyList()
    }
}

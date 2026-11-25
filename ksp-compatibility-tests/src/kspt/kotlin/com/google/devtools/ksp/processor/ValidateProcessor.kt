/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/ValidateProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate

class ValidateProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    private fun validate(
        symbol: KSDeclaration,
        predicate: (KSNode?, KSNode) -> Boolean = { _, _ -> true },
    ) {
        if (symbol.validate(predicate, apiFeatures)) {
            results.add("${symbol.simpleName.asString()} valid")
        } else {
            results.add("${symbol.simpleName.asString()} invalid")
        }
    }

    override fun toResult(): List<String> = results

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val ErrorInMember = resolver.getClassDeclarationByName("ErrorInMember")!!
        val SkipErrorInMember = resolver.getClassDeclarationByName("SkipErrorInMember")!!
        val GoodClass = resolver.getClassDeclarationByName("GoodClass")!!
        val C = resolver.getClassDeclarationByName("C")!!
        val BadJavaClass = resolver.getClassDeclarationByName("BadJavaClass")!!
        val ErrorAnnotationType = resolver.getClassDeclarationByName("ErrorAnnotationType")!!
        val ErrorInAnnotationArgumentSingleType =
            resolver.getClassDeclarationByName("ErrorInAnnotationArgumentSingleType")!!
        val ErrorInAnnotationArgumentMultipleTypes =
            resolver.getClassDeclarationByName("ErrorInAnnotationArgumentMultipleTypes")!!
        val ErrorInAnnotationArgumentComposed =
            resolver.getClassDeclarationByName("ErrorInAnnotationArgumentComposed")!!
        val ValidAnnotationArgumentType =
            resolver.getClassDeclarationByName("ValidAnnotationArgumentType")!!
        validate(ErrorInMember)
        ErrorInMember.declarations.forEach { validate(it) }
        validate(SkipErrorInMember) { node, _ ->
            node !is KSPropertyDeclaration && node !is KSFunctionDeclaration
        }
        SkipErrorInMember.declarations.forEach {
            validate(it) { node, _ ->
                node !is KSPropertyDeclaration && node !is KSFunctionDeclaration
            }
        }
        validate(GoodClass)
        validate(C)
        validate(BadJavaClass)
        validate(ErrorAnnotationType)
        validate(ErrorInAnnotationArgumentSingleType)
        validate(ErrorInAnnotationArgumentMultipleTypes)
        validate(ErrorInAnnotationArgumentComposed)
        validate(ValidAnnotationArgumentType)
        return emptyList()
    }
}

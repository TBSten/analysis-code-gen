/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/AbstractFunctionsProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class AbstractFunctionsProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    private val visitor = Visitor(apiFeatures)

    override fun toResult(): List<String> = visitor.abstractFunctionNames.sorted()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getNewFiles().forEach { it.accept(visitor, Unit) }
        return emptyList()
    }

    private class Visitor(apiFeatures: ApiFeatures) : KSTopDownVisitor<Unit, Unit>(apiFeatures) {
        val abstractFunctionNames = arrayListOf<String>()

        override fun defaultHandler(
            node: KSNode,
            data: Unit,
        ) {
        }

        override fun visitFunctionDeclaration(
            function: KSFunctionDeclaration,
            data: Unit,
        ) {
            if (function.isAbstract) {
                abstractFunctionNames += function.simpleName.asString()
            }
        }
    }
}

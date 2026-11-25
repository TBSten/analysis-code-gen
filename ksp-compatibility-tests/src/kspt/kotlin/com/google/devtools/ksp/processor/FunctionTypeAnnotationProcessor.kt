/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/FunctionTypeAnnotationProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class FunctionTypeAnnotationProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> = results

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val files = resolver.getNewFiles()

        files.forEach {
            it.accept(
                object : KSTopDownVisitor<Unit, Unit>(apiFeatures) {
                    override fun defaultHandler(
                        node: KSNode,
                        data: Unit,
                    ) = Unit

                    override fun visitPropertyDeclaration(
                        property: KSPropertyDeclaration,
                        data: Unit,
                    ) {
                        val type = property.type.resolve()
                        val propertyName = property.simpleName.asString()
                        val typeName = type.declaration.simpleName.asString()
                        results.add("$propertyName: $typeName ${type.annotations.joinToString { it.toString() }}")
                    }
                },
                Unit,
            )
        }
        return emptyList()
    }
}

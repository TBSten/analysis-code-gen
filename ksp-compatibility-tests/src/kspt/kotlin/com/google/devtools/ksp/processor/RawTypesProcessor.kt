/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/RawTypesProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class RawTypesProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    private val rawTypedEntityNames = arrayListOf<String>()

    override fun toResult(): List<String> = rawTypedEntityNames.sorted()

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val visitor =
            object : KSTopDownVisitor<Unit, Unit>(apiFeatures) {
                private val KSType.isRawType
                    get() = resolver.isJavaRawType(this)

                override fun visitFunctionDeclaration(
                    function: KSFunctionDeclaration,
                    data: Unit,
                ) {
                    function.parameters.forEach { param ->
                        val type = param.type.resolve()
                        if (type.isRawType) {
                            rawTypedEntityNames += param.name?.asString() ?: "???"
                        }
                    }
                    val returnType = function.returnType?.resolve() ?: return
                    if (returnType.isRawType) {
                        rawTypedEntityNames += function.simpleName.asString()
                    }
                }

                override fun visitPropertyDeclaration(
                    property: KSPropertyDeclaration,
                    data: Unit,
                ) {
                    val type = property.type.resolve()
                    if (type.isRawType) {
                        rawTypedEntityNames += property.simpleName.asString()
                    }
                }

                override fun defaultHandler(
                    node: KSNode,
                    data: Unit,
                ) = Unit
            }

        resolver.getDeclarationsFromPackage("").forEach { it.accept(visitor, Unit) }

        return emptyList()
    }
}

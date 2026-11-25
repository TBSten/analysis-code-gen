/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/DeclarationOrderProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*

@KspExperimental
class DeclarationOrderProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    private val result = mutableListOf<String>()

    override fun toResult() = result

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val classNames =
            listOf(
                "lib.KotlinClass",
                "lib.JavaClass",
                "KotlinClass",
                "JavaClass",
            )
        val companionClsses = listOf("KotlinCompanion", "lib.KotlinCompanion")
        val containers =
            classNames.map {
                checkNotNull(resolver.getClassDeclarationByName(it)) {
                    "cannot find $it"
                }
            } +
                companionClsses.map {
                    checkNotNull(resolver.getClassDeclarationByName(it)) {
                        "cannot find $it"
                    }.declarations.single {
                        it is KSClassDeclaration && it.isCompanionObject
                    } as KSClassDeclaration
                }
        containers.forEach { klass ->
            result.add(klass.qualifiedName!!.asString())
            result.addAll(
                resolver.getDeclarationsInSourceOrder(klass).filterIsInstance<KSPropertyDeclaration>().map {
                    it.toSignature(resolver)
                },
            )
            result.addAll(
                resolver
                    .getDeclarationsInSourceOrder(klass)
                    .filterIsInstance<KSFunctionDeclaration>()
                    .filter { !it.isConstructor() }
                    .map {
                        it.toSignature(resolver)
                    },
            )
        }
        result.addAll(
            resolver.getDeclarationsInSourceOrder(resolver.getClassDeclarationByName("kotlin.Any")!!).map {
                it.toSignature(resolver)
            },
        )
        return emptyList()
    }

    private fun KSDeclaration.toSignature(resolver: Resolver) = "${simpleName.asString()}:${resolver.mapToJvmSignature(this)}"
}

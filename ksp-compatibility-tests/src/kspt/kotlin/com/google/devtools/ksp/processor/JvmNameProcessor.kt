/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/JvmNameProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class JvmNameProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> = results

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        listOf("TestDataClass", "TestLibDataClass").forEach { clsName ->
            resolver.getClassDeclarationByName(clsName)?.let { cls ->
                results.add(
                    cls
                        .getAllProperties()
                        .map {
                            "(${it.getter?.let { resolver.getJvmName(it) }}, " +
                                "${it.setter?.let { resolver.getJvmName(it) }})"
                        }.toList()
                        .joinToString(),
                )
            }
        }
        listOf("MyAnnotationUser", "MyAnnotationUserLib").forEach { clsName ->
            resolver.getClassDeclarationByName(clsName)!!.let { cls ->
                cls.annotations.forEach { annotation ->
                    results.add(annotation.arguments.joinToString { it.name!!.asString() })
                }
            }
        }
        listOf("MyAnnotation", "MyAnnotationLib").forEach { clsName ->
            resolver.getClassDeclarationByName(clsName)!!.getAllProperties().forEach { p ->
                p.getter?.let {
                    results.add("JvmName: ${resolver.getJvmName(it)}")
                }
            }
        }
        return emptyList()
    }
}

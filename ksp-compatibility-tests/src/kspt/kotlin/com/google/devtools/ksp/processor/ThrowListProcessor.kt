/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/ThrowListProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType

@KspExperimental
class ThrowListProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> = result

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val klass = resolver.getClassDeclarationByName("ThrowsKt")!!
        val jlass = resolver.getClassDeclarationByName("ThrowsException")!!
        result.add(
            resolver
                .getJvmCheckedException(
                    klass.declarations.single {
                        it.simpleName.asString() == "throwsKT"
                    } as KSFunctionDeclaration,
                ).toResult(),
        )
        result.add(
            resolver
                .getJvmCheckedException(
                    (
                        jlass.declarations.single {
                            it.simpleName.asString() == "foo"
                        } as KSFunctionDeclaration
                    ),
                ).toResult(),
        )
        result.add(
            resolver
                .getJvmCheckedException(
                    (
                        jlass.declarations.single {
                            it.simpleName.asString() == "method"
                        } as KSFunctionDeclaration
                    ),
                ).toResult(),
        )
        val propertyA = klass.declarations.single { it.simpleName.asString() == "a" } as KSPropertyDeclaration
        result.add(resolver.getJvmCheckedException(propertyA.getter!!).toResult())
        result.add(resolver.getJvmCheckedException(propertyA.setter!!).toResult())
        klass.declarations
            .filter { it.simpleName.asString() == "syntheticAccessors" }
            .map {
                resolver.getJvmCheckedException((it as KSPropertyDeclaration).getter!!).toResult()
            }.sorted()
            .forEach { result.add(it) }
        klass.declarations
            .filter { it.simpleName.asString() == "syntheticAccessors" }
            .map {
                resolver.getJvmCheckedException((it as KSPropertyDeclaration).setter!!).toResult()
            }.sorted()
            .forEach { result.add(it) }
        val jlib = resolver.getClassDeclarationByName("JavaLib")!!
        val klib = resolver.getClassDeclarationByName("KtLib")!!
        klib.declarations
            .filter { it.simpleName.asString() == "throwsLibKt" }
            .map {
                resolver.getJvmCheckedException(it as KSFunctionDeclaration).toResult()
            }.sorted()
            .forEach { result.add(it) }
        klib.declarations
            .filter { it.simpleName.asString() == "getterThrows" }
            .map {
                resolver.getJvmCheckedException((it as KSPropertyDeclaration).getter!!).toResult()
            }.sorted()
            .forEach { result.add(it) }
        klib.declarations
            .filter { it.simpleName.asString() == "setterThrows" }
            .map {
                resolver.getJvmCheckedException((it as KSPropertyDeclaration).setter!!).toResult()
            }.sorted()
            .forEach { result.add(it) }
        klib.declarations
            .filter { it.simpleName.asString() == "bothThrows" }
            .map {
                resolver.getJvmCheckedException((it as KSPropertyDeclaration).getter!!).toResult()
            }.sorted()
            .forEach { result.add(it) }
        klib.declarations
            .filter { it.simpleName.asString() == "bothThrows" }
            .map {
                resolver.getJvmCheckedException((it as KSPropertyDeclaration).setter!!).toResult()
            }.sorted()
            .forEach { result.add(it) }
        klib.declarations
            .filter { it.simpleName.asString() == "syntheticAccessors" }
            .map {
                resolver.getJvmCheckedException((it as KSPropertyDeclaration).getter!!).toResult()
            }.sorted()
            .forEach { result.add(it) }
        klib.declarations
            .filter { it.simpleName.asString() == "syntheticAccessors" }
            .map {
                resolver.getJvmCheckedException((it as KSPropertyDeclaration).setter!!).toResult()
            }.sorted()
            .forEach { result.add(it) }
        jlib.declarations
            .filter { it.simpleName.asString() == "foo" }
            .map {
                resolver.getJvmCheckedException(it as KSFunctionDeclaration).toResult()
            }.sorted()
            .forEach { result.add(it) }
        jlib
            .getConstructors()
            .map {
                resolver.getJvmCheckedException(it).toResult()
            }.sorted()
            .forEach { result.add(it) }
        jlib.declarations
            .filter { it.simpleName.asString() == "throwsNested" }
            .map {
                resolver.getJvmCheckedException(it as KSFunctionDeclaration).toResult()
            }.sorted()
            .forEach { result.add(it) }
        return emptyList()
    }

    private fun Sequence<KSType>.toResult() =
        this.joinToString(separator = ",") {
            it.declaration.qualifiedName!!.asString()
        }
}

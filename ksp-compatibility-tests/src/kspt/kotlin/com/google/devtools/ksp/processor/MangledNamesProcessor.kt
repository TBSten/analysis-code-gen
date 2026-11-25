/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/MangledNamesProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSTopDownVisitor

@KspExperimental
@Suppress("unused") // used by the test code
class MangledNamesProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    private val results = mutableListOf<String>()

    override fun toResult() = results

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val mangleSourceNames = mutableMapOf<String, String?>()
        resolver.getAllFiles().sortedBy { it.fileName }.forEach {
            it.accept(MangledNamesVisitor(resolver, apiFeatures), mangleSourceNames)
        }
        val mangledDependencyNames = LinkedHashMap<String, String?>()
        // also collect results from library dependencies to ensure we resolve module name property
        resolver.getClassDeclarationByName("libPackage.Foo")?.accept(
            MangledNamesVisitor(resolver, apiFeatures),
            mangledDependencyNames,
        )
        resolver.getClassDeclarationByName("libPackage.AbstractKotlinClass")?.accept(
            MangledNamesVisitor(resolver, apiFeatures),
            mangledDependencyNames,
        )
        resolver.getClassDeclarationByName("libPackage.MyInterface")?.accept(
            MangledNamesVisitor(resolver, apiFeatures),
            mangledDependencyNames,
        )
        results.addAll(
            mangleSourceNames.entries.map { (decl, name) ->
                "$decl -> $name"
            },
        )
        results.addAll(
            mangledDependencyNames.entries.map { (decl, name) ->
                "$decl -> $name"
            },
        )
        return emptyList()
    }

    private class MangledNamesVisitor(val resolver: Resolver, apiFeatures: ApiFeatures) : KSTopDownVisitor<MutableMap<String, String?>, Unit>(
        apiFeatures,
    ) {
        override fun defaultHandler(
            node: KSNode,
            data: MutableMap<String, String?>,
        ) {
        }

        override fun visitDeclarationContainer(
            declarationContainer: KSDeclarationContainer,
            data: MutableMap<String, String?>,
        ) {
            declarationContainer.declarations.sortedBy { it.simpleName.asString() }.forEach { it.accept(this, data) }
        }

        override fun visitClassDeclaration(
            classDeclaration: KSClassDeclaration,
            data: MutableMap<String, String?>,
        ) {
            if (classDeclaration.modifiers.contains(Modifier.INLINE)) {
                // do not visit inline classes
                return
            }
            // put a header for readable output
            data[classDeclaration.qualifiedName!!.asString()] = "declarations"
            super.visitClassDeclaration(classDeclaration, data)
        }

        override fun visitFunctionDeclaration(
            function: KSFunctionDeclaration,
            data: MutableMap<String, String?>,
        ) {
            if (function.simpleName.asString() in IGNORED_FUNCTIONS) return
            super.visitFunctionDeclaration(function, data)
            data[function.simpleName.asString()] = resolver.getJvmName(function)
        }

        override fun visitPropertyGetter(
            getter: KSPropertyGetter,
            data: MutableMap<String, String?>,
        ) {
            super.visitPropertyGetter(getter, data)
            data["get-${getter.receiver.simpleName.asString()}"] = resolver.getJvmName(getter)
        }

        override fun visitPropertySetter(
            setter: KSPropertySetter,
            data: MutableMap<String, String?>,
        ) {
            super.visitPropertySetter(setter, data)
            data["set-${setter.receiver.simpleName.asString()}"] = resolver.getJvmName(setter)
        }

        companion object {
            // do not report these functions as they are generated only in byte code and do not affect the test.
            val IGNORED_FUNCTIONS = listOf("equals", "hashCode", "toString")
        }
    }
}

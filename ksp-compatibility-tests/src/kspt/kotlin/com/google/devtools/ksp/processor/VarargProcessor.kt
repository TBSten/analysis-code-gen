/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/VarargProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class VarargProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> = results

    override fun process(resolver: Resolver): List<KSAnnotated> {
        for (clsName in listOf("s.K", "s.J", "l.K", "l.J")) {
            val clsDecl = resolver.getClassDeclarationByName(clsName)!!
            val func = clsDecl.getDeclaredFunctions().single { it.simpleName.asString() == "foo" }
            val funcName = func.qualifiedName!!.asString()
            val param = func.parameters.single()
            val paramName = param.name!!.asString()
            val paramType = param.type.resolve()
            val isVararg = if (param.isVararg) "vararg " else ""
            results.add("$funcName($isVararg$paramName: $paramType)")
        }
        return emptyList()
    }
}

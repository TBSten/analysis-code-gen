/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/GetAnnotationByTypeProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

annotation class KotlinAnnotationWithInnerDefaults(
    val innerAnnotationVal: InnerAnnotation = InnerAnnotation(innerAnnotationDefault = 7),
) {
    annotation class InnerAnnotation(
        val innerAnnotationDefault: Int,
        val moreInnerAnnotation: MoreInnerAnnotation = MoreInnerAnnotation("OK"),
    ) {
        annotation class MoreInnerAnnotation(val moreInnerAnnotationDefault: String)
    }
}

class GetAnnotationByTypeProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    private val annotationKClass = KotlinAnnotationWithInnerDefaults::class

    override fun toResult(): List<String> = results

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val decl =
            resolver
                .getAllFiles()
                .single()
                .declarations
                .single { it.simpleName.asString() == "A" } as KSClassDeclaration
        val anno = decl.getAnnotationsByType(annotationKClass).first()
        results.add(anno.innerAnnotationVal.toString())
        return emptyList()
    }
}

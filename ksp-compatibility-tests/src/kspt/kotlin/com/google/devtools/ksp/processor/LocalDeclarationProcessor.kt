/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/LocalDeclarationProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSEmptyVisitor

class LocalDeclarationProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> = result

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val visitor = LocalDeclarationVisitor(apiFeatures)
        resolver.getAllFiles().forEach { it.accept(visitor, result) }
        return emptyList()
    }
}

class LocalDeclarationVisitor(apiFeatures: ApiFeatures) : KSEmptyVisitor<MutableList<String>, Unit>(apiFeatures) {
    override fun defaultHandler(
        node: KSNode,
        data: MutableList<String>,
    ) {
        data.add(node.toString())
    }

    override fun visitFile(
        file: KSFile,
        data: MutableList<String>,
    ) {
        super.visitFile(file, data)
        file.declarations.forEach { it.accept(this, data) }
    }

    override fun visitClassDeclaration(
        classDeclaration: KSClassDeclaration,
        data: MutableList<String>,
    ) {
        super.visitClassDeclaration(classDeclaration, data)
        classDeclaration.declarations.forEach { it.accept(this, data) }
    }

    override fun visitFunctionDeclaration(
        function: KSFunctionDeclaration,
        data: MutableList<String>,
    ) {
        super.visitFunctionDeclaration(function, data)
        function.declarations.forEach { it.accept(this, data) }
    }

    override fun visitPropertyGetter(
        getter: KSPropertyGetter,
        data: MutableList<String>,
    ) {
        getter.declarations.forEach { it.accept(this, data) }
    }

    override fun visitPropertySetter(
        setter: KSPropertySetter,
        data: MutableList<String>,
    ) {
        setter.declarations.forEach { it.accept(this, data) }
    }

    override fun visitPropertyDeclaration(
        property: KSPropertyDeclaration,
        data: MutableList<String>,
    ) {
        super.visitPropertyDeclaration(property, data)
        property.getter?.accept(this, data)
        property.setter?.accept(this, data)
    }
}

/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:common-util/src/main/kotlin/com/google/devtools/ksp/common/impl/RefPosition.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更
package me.tbsten.analysis.code.gen.ksp.bridge.common.impl

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSCallableReference
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSReferenceElement
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference

enum class RefPosition {
    PARAMETER_TYPE,
    RETURN_TYPE,
    SUPER_TYPE
}

// TODO: Strict mode for catching unhandled cases.
fun findRefPosition(ref: KSTypeReference): RefPosition = when (val parent = ref.parent) {
    is KSCallableReference -> when (ref) {
        parent.returnType -> RefPosition.RETURN_TYPE
        else -> RefPosition.PARAMETER_TYPE
    }
    is KSFunctionDeclaration -> when (ref) {
        parent.returnType -> RefPosition.RETURN_TYPE
        else -> RefPosition.PARAMETER_TYPE
    }
    is KSPropertyGetter -> RefPosition.RETURN_TYPE
    is KSPropertyDeclaration -> when (ref) {
        parent.type -> RefPosition.RETURN_TYPE
        else -> RefPosition.PARAMETER_TYPE
    }
    is KSClassDeclaration -> RefPosition.SUPER_TYPE
    // is KSTypeArgument -> RefPosition.PARAMETER_TYPE
    // is KSAnnotation -> RefPosition.PARAMETER_TYPE
    // is KSTypeAlias -> RefPosition.PARAMETER_TYPE
    // is KSValueParameter -> RefPosition.PARAMETER_TYPE
    // is KSTypeParameter -> RefPosition.PARAMETER_TYPE
    else -> RefPosition.PARAMETER_TYPE
}

// Search in self and parents for the first type reference that is not part of a type argument.
fun KSTypeReference.findOuterMostRef(): Pair<KSTypeReference, List<Int>> {
    fun KSNode.findParentRef(): KSTypeReference? {
        var parent = parent
        while (parent != null && parent !is KSTypeReference)
            parent = parent.parent
        return parent
    }

    val fallback = Pair<KSTypeReference, List<Int>>(this, emptyList())
    val indexes = mutableListOf<Int>()
    var candidate: KSTypeReference = this
    // KSTypeArgument's parent can be either KSReferenceElement or KSType.
    while (candidate.parent is KSTypeArgument) {
        // If the parent is a KSType, it's a synthetic reference.
        // Do nothing and reply on the fallback behavior.
        val referenceElement = (candidate.parent!!.parent as? KSReferenceElement) ?: return fallback
        indexes.add(referenceElement.typeArguments.indexOf(candidate.parent))
        // In case the program isn't properly structured, fallback.
        candidate = referenceElement.findParentRef() ?: return fallback
    }
    return Pair(candidate, indexes)
}

fun KSTypeReference.isReturnTypeOfAnnotationMethod(): Boolean {
    var candidate = this.parent
    while (candidate !is KSClassDeclaration && candidate != null)
        candidate = candidate.parent
    return candidate?.classKind == ClassKind.ANNOTATION_CLASS
}

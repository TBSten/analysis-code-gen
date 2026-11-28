/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:common-util/src/main/kotlin/com/google/devtools/ksp/common/KSPUtils.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更
package me.tbsten.analysis.code.gen.ksp.bridge.common

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSType

@Suppress("NOTHING_TO_INLINE")
private inline fun Any?.idHashCode() = System.identityHashCode(this)

class IdKey<T>(private val k: T) {
    override fun equals(other: Any?): Boolean = if (other is IdKey<*>) k === other.k else false
    override fun hashCode(): Int = k.idHashCode()
}

class IdKeyPair<T, P>(private val k1: T, private val k2: P) {
    override fun equals(other: Any?): Boolean = if (other is IdKeyPair<*, *>) k1 === other.k1 &&
        k2 === other.k2 else false
    override fun hashCode(): Int = k1.idHashCode() * 31 + k2.idHashCode()
}

class IdKeyTriple<T, P, Q>(private val k1: T, private val k2: P, private val k3: Q) {
    override fun equals(other: Any?): Boolean = if (other is IdKeyTriple<*, *, *>) k1 === other.k1 &&
        k2 === other.k2 && k3 === other.k3 else false
    override fun hashCode(): Int = k1.idHashCode() * 31 * 31 + k2.idHashCode() * 31 + k3.idHashCode()
}

@Suppress("UNCHECKED_CAST")
fun extractThrowsAnnotation(annotated: KSAnnotated): Sequence<KSType> {
    return annotated.annotations
        .singleOrNull {
            it.shortName.asString() == "Throws" &&
                it.annotationType.resolve().declaration.qualifiedName?.asString()?.let {
                it == "kotlin.jvm.Throws" || it == "kotlin.Throws"
            } ?: false
        }?.arguments
        ?.singleOrNull()
        ?.let { it.value as? ArrayList<KSType> }
        ?.asSequence() ?: emptySequence()
}

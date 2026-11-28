/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:common-util/src/main/kotlin/com/google/devtools/ksp/common/impl/KSTypeReferenceSyntheticImpl.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更
package me.tbsten.analysis.code.gen.ksp.bridge.common.impl

import me.tbsten.analysis.code.gen.ksp.bridge.common.IdKey
import me.tbsten.analysis.code.gen.ksp.bridge.common.KSObjectCache
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSReferenceElement
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.NonExistLocation
import com.google.devtools.ksp.symbol.Origin

class KSTypeReferenceSyntheticImpl(val ksType: KSType, override val parent: KSNode?) : KSTypeReference {
    companion object : KSObjectCache<Pair<IdKey<KSType>, KSNode?>, KSTypeReferenceSyntheticImpl>() {
        fun getCached(ksType: KSType, parent: KSNode?) = KSTypeReferenceSyntheticImpl.cache
            .getOrPut(Pair(IdKey(ksType), parent)) { KSTypeReferenceSyntheticImpl(ksType, parent) }
    }

    override val annotations: Sequence<KSAnnotation> = emptySequence()

    override val element: KSReferenceElement? = null

    override val location: Location = NonExistLocation

    override val modifiers: Set<Modifier> = emptySet()

    override val origin: Origin = Origin.SYNTHETIC

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }

    override fun resolve(): KSType {
        return ksType
    }

    override fun toString(): String {
        return ksType.toString()
    }
}

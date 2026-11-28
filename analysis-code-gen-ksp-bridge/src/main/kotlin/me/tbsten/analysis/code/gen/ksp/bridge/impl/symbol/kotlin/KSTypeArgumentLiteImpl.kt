/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/symbol/kotlin/KSTypeArgumentLiteImpl.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更
package me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin

import me.tbsten.analysis.code.gen.ksp.bridge.common.KSObjectCache
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.NonExistLocation
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.Variance

class KSTypeArgumentLiteImpl private constructor(override val type: KSTypeReference, override val variance: Variance) :
    KSTypeArgument, Deferrable {
    companion object : KSObjectCache<Pair<KSTypeReference, Variance>, KSTypeArgument>() {
        fun getCached(type: KSTypeReference, variance: Variance) = cache.getOrPut(Pair(type, variance)) {
            KSTypeArgumentLiteImpl(type, variance)
        }
    }

    override val annotations: Sequence<KSAnnotation> = emptySequence()

    override val origin: Origin = Origin.SYNTHETIC

    override val location: Location = NonExistLocation

    override val parent: KSNode? = null

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeArgument(this, data)
    }

    override fun defer(): Restorable? {
        TODO("Not yet implemented")
    }
}

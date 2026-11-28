/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/symbol/kotlin/KSDynamicReferenceImpl.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更
package me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin

import me.tbsten.analysis.code.gen.ksp.bridge.common.KSObjectCache
import com.google.devtools.ksp.symbol.*

class KSDynamicReferenceImpl private constructor(override val parent: KSNode) : KSDynamicReference {
    companion object : KSObjectCache<KSTypeReference, KSDynamicReferenceImpl>() {
        fun getCached(parent: KSTypeReference) = cache.getOrPut(parent) { KSDynamicReferenceImpl(parent) }
    }

    override val origin = Origin.KOTLIN

    override val location: Location by lazy {
        NonExistLocation
    }

    override val typeArguments: List<KSTypeArgument> = emptyList()

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitDynamicReference(this, data)
    }

    override fun toString(): String {
        return "<dynamic type>"
    }
}

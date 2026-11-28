/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/symbol/kotlin/KSValueParameterLiteImpl.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更
package me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin

import me.tbsten.analysis.code.gen.ksp.bridge.common.IdKeyPair
import me.tbsten.analysis.code.gen.ksp.bridge.common.KSObjectCache
import me.tbsten.analysis.code.gen.ksp.bridge.common.impl.KSNameImpl
import me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.types.KaType

class KSValueParameterLiteImpl private constructor(ktType: KaType, override val parent: KSNode) :
    KSValueParameter {
    companion object : KSObjectCache<IdKeyPair<KaType, KSNode>, KSValueParameter>() {
        fun getCached(ktType: KaType, parent: KSNode): KSValueParameter = cache.getOrPut(IdKeyPair(ktType, parent)) {
            KSValueParameterLiteImpl(ktType, parent)
        }
    }

    // preferably maybe use empty name to match compiler, but use underscore to match FE1.0 implementation.
    override val name: KSName = KSNameImpl.getCached("_")

    override val type: KSTypeReference = KSTypeReferenceResolvedImpl.getCached(ktType)

    override val isVararg: Boolean = false

    override val isNoInline: Boolean = false

    override val isCrossInline: Boolean = false

    override val isVal: Boolean = false

    override val isVar: Boolean = false

    override val hasDefault: Boolean = false

    override val annotations: Sequence<KSAnnotation> = emptySequence()

    override val origin: Origin = parent.origin

    override val location: Location = parent.location

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueParameter(this, data)
    }
}

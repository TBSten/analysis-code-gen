/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/symbol/kotlin/KSCallableReferenceImpl.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更
package me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin

import me.tbsten.analysis.code.gen.ksp.bridge.common.IdKeyPair
import me.tbsten.analysis.code.gen.ksp.bridge.common.KSObjectCache
import me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin.resolved.KSTypeArgumentResolvedImpl
import me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.abbreviationOrSelf

// TODO: implement a psi based version, rename this class to resolved Impl.
class KSCallableReferenceImpl private constructor(
    private val ktFunctionalType: KaFunctionType,
    override val parent: KSNode?
) : KSCallableReference {
    companion object : KSObjectCache<IdKeyPair<KaType, KSNode?>, KSCallableReference>() {
        fun getCached(ktFunctionalType: KaFunctionType, parent: KSNode?): KSCallableReference =
            cache.getOrPut(IdKeyPair(ktFunctionalType, parent)) { KSCallableReferenceImpl(ktFunctionalType, parent) }
    }
    override val receiverType: KSTypeReference?
        get() = ktFunctionalType.receiverType?.abbreviationOrSelf?.let { KSTypeReferenceResolvedImpl.getCached(it) }

    override val functionParameters: List<KSValueParameter>
        get() = ktFunctionalType.parameterTypes.map {
            KSValueParameterLiteImpl.getCached(it, this@KSCallableReferenceImpl)
        }

    override val returnType: KSTypeReference
        get() = KSTypeReferenceResolvedImpl.getCached(ktFunctionalType.returnType.abbreviationOrSelf)

    override val typeArguments: List<KSTypeArgument>
        get() = ktFunctionalType.typeArguments().map { KSTypeArgumentResolvedImpl.getCached(it, this) }

    override val origin: Origin
        get() = parent?.origin ?: Origin.SYNTHETIC

    override val location: Location
        get() = parent?.location ?: NonExistLocation
}

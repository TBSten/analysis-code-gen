/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/symbol/kotlin/KSDefNonNullReferenceImpl.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更
package me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin

import me.tbsten.analysis.code.gen.ksp.bridge.common.IdKeyPair
import me.tbsten.analysis.code.gen.ksp.bridge.common.KSObjectCache
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.types.KaDefinitelyNotNullType

class KSDefNonNullReferenceImpl private constructor(
    private val ktDefinitelyNotNullType: KaDefinitelyNotNullType,
    override val parent: KSTypeReference?
) : KSDefNonNullReference {
    companion object : KSObjectCache<IdKeyPair<KaDefinitelyNotNullType, KSTypeReference?>, KSDefNonNullReference>() {
        fun getCached(ktType: KaDefinitelyNotNullType, parent: KSTypeReference?) =
            KSDefNonNullReferenceImpl.cache
                .getOrPut(IdKeyPair(ktType, parent)) { KSDefNonNullReferenceImpl(ktType, parent) }
    }
    override val enclosedType: KSClassifierReference by lazy {
        ktDefinitelyNotNullType.original.toClassifierReference(parent) as KSClassifierReference
    }
    override val typeArguments: List<KSTypeArgument>
        get() = emptyList()

    override val origin: Origin = Origin.KOTLIN

    override val location: Location
        get() = parent?.location ?: NonExistLocation

    override fun toString() = "${enclosedType.referencedName()} & Any"
}

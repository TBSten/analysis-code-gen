/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/symbol/kotlin/resolved/KSClassifierReferenceResolvedImpl.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更
package me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin.resolved

import me.tbsten.analysis.code.gen.ksp.bridge.common.IdKeyPair
import me.tbsten.analysis.code.gen.ksp.bridge.common.IdKeyTriple
import me.tbsten.analysis.code.gen.ksp.bridge.common.KSObjectCache
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType

class KSClassifierReferenceResolvedImpl private constructor(
    private val ktType: KaClassType,
    private val index: Int,
    override val parent: KSTypeReference?
) : KSClassifierReference {
    companion object :
        KSObjectCache<IdKeyTriple<KaClassType, Int, KSTypeReference?>, KSClassifierReferenceResolvedImpl>() {
        fun getCached(ktType: KaClassType, index: Int, parent: KSTypeReference?) =
            cache.getOrPut(IdKeyTriple(ktType, index, parent)) {
                KSClassifierReferenceResolvedImpl(ktType, index, parent)
            }
    }

    private val classifierReference: KaClassTypeQualifier
        get() = ktType.qualifiers[index]

    override val qualifier: KSClassifierReference? by lazy {
        if (index == 0) {
            null
        } else {
            getCached(ktType, index - 1, parent)
        }
    }

    override fun referencedName(): String {
        return classifierReference.name.asString()
    }

    override val typeArguments: List<KSTypeArgument> by lazy {
        classifierReference.typeArguments.map { KSTypeArgumentResolvedImpl.getCached(it, this) }
    }

    override val origin: Origin = parent?.origin ?: Origin.SYNTHETIC

    override val location: Location
        get() = parent?.location ?: NonExistLocation

    override fun toString(): String {
        return referencedName() + if (typeArguments.isNotEmpty()) "<${
        typeArguments.joinToString(", ") { it.toString() }
        }>" else ""
    }
}

class KSClassifierParameterImpl private constructor(
    private val ktType: KaTypeParameterType,
    override val parent: KSTypeReference?
) : KSClassifierReference {
    companion object : KSObjectCache<IdKeyPair<KaTypeParameterType, KSTypeReference?>, KSClassifierParameterImpl>() {
        fun getCached(ktType: KaTypeParameterType, parent: KSTypeReference?) =
            KSClassifierParameterImpl.cache.getOrPut(IdKeyPair(ktType, parent)) {
                KSClassifierParameterImpl(ktType, parent)
            }
    }

    override val qualifier: KSClassifierReference? = null

    override fun referencedName(): String {
        return ktType.name.asString()
    }

    override val typeArguments: List<KSTypeArgument>
        get() = emptyList()
    override val origin: Origin
        get() = parent?.origin ?: Origin.SYNTHETIC
    override val location: Location
        get() = parent?.location ?: NonExistLocation

    override fun toString(): String {
        return referencedName()
    }
}

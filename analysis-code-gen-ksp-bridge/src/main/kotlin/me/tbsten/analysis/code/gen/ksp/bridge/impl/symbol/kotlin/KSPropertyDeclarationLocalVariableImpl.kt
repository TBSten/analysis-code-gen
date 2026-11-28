/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/symbol/kotlin/KSPropertyDeclarationLocalVariableImpl.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更
package me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin

import me.tbsten.analysis.code.gen.ksp.bridge.common.KSObjectCache
import me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.symbol.KSExpectActual
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import org.jetbrains.kotlin.analysis.api.symbols.KaLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.abbreviationOrSelf
import org.jetbrains.kotlin.psi.KtProperty

class KSPropertyDeclarationLocalVariableImpl private constructor(
    private val ktLocalVariableSymbol: KaLocalVariableSymbol
) : KSPropertyDeclaration,
    AbstractKSDeclarationImpl(),
    KSExpectActual by KSExpectActualImpl(ktLocalVariableSymbol) {
    override val ktDeclarationSymbol get() = ktLocalVariableSymbol
    companion object : KSObjectCache<KaLocalVariableSymbol, KSPropertyDeclarationLocalVariableImpl>() {
        fun getCached(ktLocalVariableSymbol: KaLocalVariableSymbol) =
            cache.getOrPut(ktLocalVariableSymbol) { KSPropertyDeclarationLocalVariableImpl(ktLocalVariableSymbol) }
    }

    override val getter: KSPropertyGetter? = null

    override val setter: KSPropertySetter? = null

    override val extensionReceiver: KSTypeReference? = null

    override val type: KSTypeReference by lazy {
        (ktLocalVariableSymbol.psiIfSource() as? KtProperty)?.typeReference
            ?.let { KSTypeReferenceImpl.getCached(it, this) }
            ?: KSTypeReferenceResolvedImpl.getCached(ktLocalVariableSymbol.returnType.abbreviationOrSelf, this)
    }

    override val isMutable: Boolean = !ktLocalVariableSymbol.isVal

    override val hasBackingField: Boolean = false

    override fun isDelegated(): Boolean = false

    override fun findOverridee() = null

    override fun asMemberOf(containing: KSType): KSType {
        TODO("Not yet implemented")
    }

    override val qualifiedName: KSName? = null

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }

    override fun defer(): Restorable? {
        return ktLocalVariableSymbol.defer(::getCached)
    }
}

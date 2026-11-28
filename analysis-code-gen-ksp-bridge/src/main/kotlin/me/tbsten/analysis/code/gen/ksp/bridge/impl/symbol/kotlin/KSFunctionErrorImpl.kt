/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/symbol/kotlin/KSFunctionErrorImpl.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更
package me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin

import com.google.devtools.ksp.symbol.KSFunction
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter

class KSFunctionErrorImpl(
    private val declaration: KSFunctionDeclaration
) : KSFunction {
    override val isError: Boolean = true

    override val returnType: KSType
        get() = KSErrorType.fromReferenceBestEffort(declaration.returnType)

    override val parameterTypes: List<KSType?>
        get() = declaration.parameters.map {
            KSErrorType.fromReferenceBestEffort(it.type)
        }
    override val typeParameters: List<KSTypeParameter>
        get() = emptyList()

    override val extensionReceiverType: KSType?
        get() = declaration.extensionReceiver?.let {
            KSErrorType.fromReferenceBestEffort(it)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KSFunctionErrorImpl

        if (declaration != other.declaration) return false

        return true
    }

    override fun hashCode(): Int {
        return declaration.hashCode()
    }
}

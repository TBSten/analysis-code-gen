/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/symbol/kotlin/synthetic/KSSyntheticAnnotations.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名を変更。AA 2.4.21 の KaAnnotationImpl の引数に合わせた
package me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin.synthetic

import me.tbsten.analysis.code.gen.ksp.bridge.impl.ResolverAAImpl
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaAnnotationImpl
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinAlwaysAccessibleLifetimeToken
import org.jetbrains.kotlin.name.ClassId

@OptIn(KaImplementationDetail::class, KaPlatformInterface::class)
fun getExtensionFunctionTypeAnnotation() = KaAnnotationImpl(
    ClassId.fromString(ExtensionFunctionType::class.qualifiedName!!),
    null,
    lazyOf(emptyList()),
    null,
    KotlinAlwaysAccessibleLifetimeToken(ResolverAAImpl.ktModule.project)
)

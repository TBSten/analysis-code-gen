/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/symbol/kotlin/AbstractKSDeclarationImpl.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更
package me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin

import me.tbsten.analysis.code.gen.ksp.bridge.InternalKSPException
import me.tbsten.analysis.code.gen.ksp.bridge.common.impl.KSNameImpl
import me.tbsten.analysis.code.gen.ksp.bridge.common.lazyMemoizedSequence
import me.tbsten.analysis.code.gen.ksp.bridge.common.toKSModifiers
import me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.java.KSAnnotationJavaImpl
import me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.util.toKSModifiers
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJvmModifiersOwner
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtModifierListOwner

abstract class AbstractKSDeclarationImpl : KSDeclaration, Deferrable {
    abstract val ktDeclarationSymbol: KaDeclarationSymbol

    override val origin: Origin by lazy {
        mapAAOrigin(ktDeclarationSymbol)
    }

    override val location: Location by lazy {
        ktDeclarationSymbol.psi.toLocation()
    }

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached((ktDeclarationSymbol as? KaNamedSymbol)?.name?.asString() ?: "")
    }

    override val annotations: Sequence<KSAnnotation>
        get() = originalAnnotations

    override val modifiers: Set<Modifier> by lazy {
        if (origin == Origin.JAVA_LIB || origin == Origin.KOTLIN_LIB || origin == Origin.SYNTHETIC) {
            when (val ktDeclarationSymbol = this.ktDeclarationSymbol) {
                is KaPropertySymbol -> ktDeclarationSymbol.toModifiers()
                is KaClassSymbol -> ktDeclarationSymbol.toModifiers()
                is KaFunctionSymbol -> ktDeclarationSymbol.toModifiers()
                is KaJavaFieldSymbol -> ktDeclarationSymbol.toModifiers()
                is KaTypeAliasSymbol -> ktDeclarationSymbol.toModifiers()
                else -> throw InternalKSPException(
                    "Unexpected symbol type: $ktDeclarationSymbol",
                    this.location,
                    ktDeclarationSymbol.javaClass
                )
            }
        } else {
            when (val psi = ktDeclarationSymbol.psi) {
                is KtModifierListOwner -> psi.toKSModifiers()
                is PsiModifierListOwner -> psi.toKSModifiers()
                null -> emptySet() // Kotlin property without backing field
                else -> throw InternalKSPException(
                    "Unexpected symbol type ${ktDeclarationSymbol.psi?.javaClass}",
                    this.location,
                    psi.javaClass
                )
            }
        }
    }

    override val containingFile: KSFile? by lazy {
        ktDeclarationSymbol.toContainingFile()
    }

    override val packageName: KSName by lazy {
        // source
        if (origin == Origin.KOTLIN || origin == Origin.JAVA) {
            containingFile?.packageName
                ?: throw InternalKSPException(
                    "Unexpected null containing file for '${qualifiedName?.asString() ?: simpleName.asString()}'",
                    location,
                    this@AbstractKSDeclarationImpl.javaClass
                )
        } else {
            // top level declaration
            when (val ktDeclarationSymbol = this.ktDeclarationSymbol) {
                is KaClassLikeSymbol -> ktDeclarationSymbol.classId?.packageFqName?.asString()
                is KaCallableSymbol -> ktDeclarationSymbol.callableId?.packageName?.asString()
                else -> null
            }?.let {
                KSNameImpl.getCached(it)
            } //  null -> non top level declaration, find in parent
                ?: ktDeclarationSymbol.getContainingKSSymbol()?.packageName
                ?: throw InternalKSPException(
                    "failed to find package name for $this",
                    this.location,
                    ktDeclarationSymbol.javaClass,
                )
        }
    }

    @OptIn(KaExperimentalApi::class)
    override val typeParameters: List<KSTypeParameter> by lazy {
        ktDeclarationSymbol.typeParameters.map { KSTypeParameterImpl.getCached(it) }
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        parent as? KSDeclaration
    }

    override val parent: KSNode? by lazy {
        analyze {
            ktDeclarationSymbol.containingSymbol?.let {
                ktDeclarationSymbol.getContainingKSSymbol()
            } ?: (ktDeclarationSymbol.psi?.parentOfType<PsiClass>(withSelf = false))?.namedClassSymbol?.let {
                KSClassDeclarationImpl.getCached(it)
            } ?: ktDeclarationSymbol.toContainingFile()
        }
    }

    override fun toString(): String {
        return simpleName.asString()
    }

    override val docString: String?
        get() = ktDeclarationSymbol.toDocString()

    internal open val originalAnnotations: Sequence<KSAnnotation> by lazyMemoizedSequence {
        if (ktDeclarationSymbol.psi is KtAnnotated) {
            (ktDeclarationSymbol.psi as KtAnnotated).annotations(ktDeclarationSymbol, this)
        } else if (
            ktDeclarationSymbol.origin == KaSymbolOrigin.JAVA_SOURCE &&
            ktDeclarationSymbol.psi is PsiJvmModifiersOwner
        ) {
            (ktDeclarationSymbol.psi as PsiJvmModifiersOwner)
                .annotations.map { KSAnnotationJavaImpl.getCached(it, this) }.asSequence()
        } else {
            ktDeclarationSymbol.annotations(this)
        }
    }
}

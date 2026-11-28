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
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/symbol/kotlin/KSAnnotationImpl.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更

package me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin

import me.tbsten.analysis.code.gen.ksp.bridge.common.IdKeyPair
import me.tbsten.analysis.code.gen.ksp.bridge.common.KSObjectCache
import me.tbsten.analysis.code.gen.ksp.bridge.common.impl.KSNameImpl
import me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin.KSAnnotationOps.findAbsentDefaultArguments
import me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin.KSAnnotationOps.findDefaultArguments
import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.NonExistLocation
import com.google.devtools.ksp.symbol.Origin
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget as KaAnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.KtAnnotationEntry

// TODO: implement a psi based version of annotation application.
class KSAnnotationImpl private constructor(
    private val ktAnnotationEntry: KtAnnotationEntry,
    override val parent: KSNode?,
    private val resolveToAnnotationApplication: () -> KaAnnotation
) : KSAnnotation {
    companion object : KSObjectCache<IdKeyPair<KtAnnotationEntry, KSNode?>, KSAnnotationImpl>() {
        fun getCached(
            ktAnnotationEntry: KtAnnotationEntry,
            parent: KSNode? = null,
            resolveToAnnotationApplication: () -> KaAnnotation
        ) =
            cache.getOrPut(IdKeyPair(ktAnnotationEntry, parent)) {
                KSAnnotationImpl(ktAnnotationEntry, parent, resolveToAnnotationApplication)
            }
    }

    private val annotationApplication by lazy {
        resolveToAnnotationApplication()
    }

    override val annotationType: KSTypeReference by lazy {
        analyze {
            ktAnnotationEntry.typeReference!!.let {
                KSTypeReferenceImpl.getCached(
                    it,
                    this@KSAnnotationImpl
                )
            }
        }
    }

    override val arguments: List<KSValueArgument> by lazy {
        val presentArgs = annotationApplication.arguments.map {
            KSValueArgumentImpl.getCached(it, this, origin)
        }
        presentArgs +
            findAbsentDefaultArguments(this, presentArgs) +
            listOfNotNull(
                KSAnnotationOps.findAbsentVararg(
                    this,
                    annotationApplication.classId,
                    annotationApplication.arguments
                )
            )
    }

    override val defaultArguments: List<KSValueArgument> by lazy {
        findDefaultArguments(this, annotationApplication.classId)
    }

    override val shortName: KSName by lazy {
        KSNameImpl.getCached(ktAnnotationEntry.shortName!!.asString())
    }

    override val useSiteTarget: AnnotationUseSiteTarget? by lazy {
        when (ktAnnotationEntry.useSiteTarget?.getAnnotationUseSiteTarget()) {
            null -> null
            KaAnnotationUseSiteTarget.FILE -> AnnotationUseSiteTarget.FILE
            KaAnnotationUseSiteTarget.PROPERTY -> AnnotationUseSiteTarget.PROPERTY
            KaAnnotationUseSiteTarget.FIELD -> AnnotationUseSiteTarget.FIELD
            KaAnnotationUseSiteTarget.PROPERTY_GETTER -> AnnotationUseSiteTarget.GET
            KaAnnotationUseSiteTarget.PROPERTY_SETTER -> AnnotationUseSiteTarget.SET
            KaAnnotationUseSiteTarget.RECEIVER -> AnnotationUseSiteTarget.RECEIVER
            KaAnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER -> AnnotationUseSiteTarget.PARAM
            KaAnnotationUseSiteTarget.SETTER_PARAMETER -> AnnotationUseSiteTarget.SETPARAM
            KaAnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD -> AnnotationUseSiteTarget.DELEGATE
            KaAnnotationUseSiteTarget.ALL -> AnnotationUseSiteTarget.ALL
        }
    }

    override val origin: Origin = Origin.KOTLIN

    override val location: Location by lazy {
        annotationApplication.psi?.toLocation() ?: NonExistLocation
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnnotation(this, data)
    }

    override fun toString(): String {
        return "@${shortName.asString()}"
    }
}

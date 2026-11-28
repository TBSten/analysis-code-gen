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
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/symbol/kotlin/KSFileJavaImpl.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更

package me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin

import me.tbsten.analysis.code.gen.ksp.bridge.common.KSObjectCache
import me.tbsten.analysis.code.gen.ksp.bridge.common.impl.KSNameImpl
import me.tbsten.analysis.code.gen.ksp.bridge.common.lazyMemoizedSequence
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Origin
import com.intellij.psi.PsiJavaFile

class KSFileJavaImpl private constructor(val psi: PsiJavaFile) : KSFile, Deferrable {
    companion object : KSObjectCache<PsiJavaFile, KSFileJavaImpl>() {
        fun getCached(psi: PsiJavaFile) = cache.getOrPut(psi) { KSFileJavaImpl(psi) }
    }

    override val packageName: KSName by lazy {
        KSNameImpl.getCached(psi.packageName)
    }

    override val fileName: String = psi.name

    override val filePath: String = psi.virtualFile.path

    override val declarations: Sequence<KSDeclaration> by lazyMemoizedSequence {
        psi.classes.asSequence().mapNotNull { psi ->
            analyze {
                psi.namedClassSymbol?.let { KSClassDeclarationImpl.getCached(it) }
            }
        }
    }

    override val origin: Origin = Origin.JAVA

    override val location: Location by lazy {
        psi.toLocation()
    }

    override val parent: KSNode? = null

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitFile(this, data)
    }

    override val annotations: Sequence<KSAnnotation> = emptySequence()

    override fun toString(): String {
        return "File: ${this.fileName}"
    }

    // Although Resolver.getSymbolsWithAnnotation never returns a java file because the latter cannot have file
    // annotations, this is used internally to restore files across rounds.
    override fun defer(): Restorable {
        return Restorable {
            analyze { getCached(psi) }
        }
    }
}

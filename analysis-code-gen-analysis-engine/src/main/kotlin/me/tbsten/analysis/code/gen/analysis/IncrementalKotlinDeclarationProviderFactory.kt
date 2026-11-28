/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/standalone/IncrementalKotlinDeclarationProviderFactory.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: ktlint で整形。パッケージ名を変更し internal にした
package me.tbsten.analysis.code.gen.analysis

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinCompositeDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProviderFactory
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeAlias

@OptIn(KaPlatformInterface::class, KaImplementationDetail::class)
internal class IncrementalKotlinDeclarationProviderFactory(
    private val project: Project,
    private val environment: CoreApplicationEnvironment,
) : KotlinDeclarationProviderFactory {
    private val staticFactories: MutableList<KotlinStandaloneDeclarationProviderFactory> = mutableListOf()

    override fun createDeclarationProvider(
        scope: GlobalSearchScope,
        contextualModule: KaModule?,
    ): KotlinDeclarationProvider {
        val providers = staticFactories.map { it.createDeclarationProvider(scope, contextualModule) }
        return KotlinCompositeDeclarationProvider.create(providers)
    }

    fun update(files: Collection<KtFile>) {
        val skipBuiltIns = staticFactories.isNotEmpty()
        val staticFactory =
            KotlinStandaloneDeclarationProviderFactory(
                project,
                environment,
                files,
                skipBuiltins = skipBuiltIns,
            )
        staticFactories.add(staticFactory)
    }

    fun getDirectInheritorCandidates(baseClassName: Name): Set<KtClassOrObject> =
        staticFactories.flatMapTo(mutableSetOf()) {
            it.getDirectInheritorCandidates(baseClassName)
        }

    fun getInheritableTypeAliases(aliasedName: Name): Set<KtTypeAlias> =
        staticFactories.flatMapTo(mutableSetOf()) {
            it.getInheritableTypeAliases(aliasedName)
        }
}

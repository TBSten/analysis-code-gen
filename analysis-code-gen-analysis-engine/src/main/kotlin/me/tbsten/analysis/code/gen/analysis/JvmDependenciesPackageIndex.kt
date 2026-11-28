/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/standalone/JvmDependenciesPackageIndex.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: ktlint で整形。パッケージ名を変更し internal にした
package me.tbsten.analysis.code.gen.analysis

import com.intellij.core.CorePackageIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.CollectionQuery
import com.intellij.util.Query
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.name.FqName

/**
 * A [CorePackageIndex] implementation that delegates package directory lookups to
 * [JvmDependenciesIndex].
 *
 * By default, [CorePackageIndex] linearly scans all classpath roots on every package lookup.
 * Delegating to [index] avoids duplicate directory traversal and reuses the package caches already
 * maintained by [IncrementalJavaFileManager]'s dependencies index.
 */
internal class JvmDependenciesPackageIndex(
    private val index: JvmDependenciesIndex,
) : CorePackageIndex() {
    override fun getDirectoriesByPackageName(
        packageName: String,
        includeLibrarySources: Boolean,
    ): Array<VirtualFile> = findDirectoriesByPackageName(packageName).toTypedArray()

    override fun getDirsByPackageName(
        packageName: String,
        includeLibrarySources: Boolean,
    ): Query<VirtualFile> = CollectionQuery(findDirectoriesByPackageName(packageName))

    private fun findDirectoriesByPackageName(packageName: String): List<VirtualFile> =
        buildList {
            index.traverseDirectoriesInPackage(FqName(packageName)) { dir, _ ->
                add(dir)
                true
            }
        }
}

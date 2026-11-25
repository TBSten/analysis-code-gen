// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// Copyright kotlin-compile-testing contributors (tschuchortdev/kotlin-compile-testing, ZacSweers/kotlin-compile-testing)
// Ported-From: ZacSweers/kotlin-compile-testing@244ec46c6e280e0a7e8ab7a65022a657bd6824e8:core/src/main/kotlin/com/tschuchort/compiletesting/HostEnvironment.kt
// Modified-By: analysis-code-gen contributors
// Modifications: パッケージを me.tbsten.analysiscodegen.testing.kct へ移動、@AutoService を手書きの META-INF/services に置換、コンパイラプラグイン ID を変更（それ以外の処理は変えていない）
package me.tbsten.analysiscodegen.testing.kct

import io.github.classgraph.ClassGraph
import java.io.File

/** Utility object to provide everything we might discover from the host environment. */
internal object HostEnvironment {
  val classpath by lazy { getHostClasspaths() }

  val kotlinStdLibJar: File? by lazy {
    findInClasspath(kotlinDependencyRegex("(kotlin-stdlib|kotlin-runtime)"))
  }

  val kotlinStdLibCommonJar: File? by lazy {
    findInClasspath(kotlinDependencyRegex("kotlin-stdlib-common"))
  }

  val kotlinStdLibJdkJar: File? by lazy {
    findInClasspath(kotlinDependencyRegex("kotlin-stdlib-jdk[0-9]+"))
  }

  val kotlinStdLibJsJar: File? by default {
    findInClasspath(kotlinDependencyRegex("kotlin-stdlib-js"))
  }

  val kotlinReflectJar: File? by lazy { findInClasspath(kotlinDependencyRegex("kotlin-reflect")) }

  val kotlinScriptRuntimeJar: File? by lazy {
    findInClasspath(kotlinDependencyRegex("kotlin-script-runtime"))
  }

  val toolsJar: File? by lazy { findInClasspath(Regex("tools.jar")) }

  private fun kotlinDependencyRegex(prefix: String): Regex {
    return Regex("$prefix(-[0-9]+\\.[0-9]+(\\.[0-9]+)?)([-0-9a-zA-Z]+)?\\.jar")
  }

  /** Tries to find a file matching the given [regex] in the host process' classpath */
  private fun findInClasspath(regex: Regex): File? {
    val jarFile = classpath.firstOrNull { classpath ->
      classpath.name.matches(regex)
      // TODO("check that jar file actually contains the right classes")
    }
    return jarFile
  }

  /** Returns the files on the classloader's classpath and modulepath */
  private fun getHostClasspaths(): List<File> {
    val classGraph = ClassGraph().enableSystemJarsAndModules().removeTemporaryFilesAfterScan()

    val classpaths = classGraph.classpathFiles
    val modules = classGraph.modules.mapNotNull { it.locationFile }

    return (classpaths + modules).distinctBy(File::getAbsolutePath)
  }
}

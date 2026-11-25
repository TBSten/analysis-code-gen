// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// Copyright kotlin-compile-testing contributors (tschuchortdev/kotlin-compile-testing, ZacSweers/kotlin-compile-testing)
// Ported-From: ZacSweers/kotlin-compile-testing@244ec46c6e280e0a7e8ab7a65022a657bd6824e8:core/src/main/kotlin/SynchronizedToolProvider.kt
// Modified-By: analysis-code-gen contributors
// Modifications: パッケージを me.tbsten.analysis.code.gen.testing.kct へ移動、@AutoService を手書きの META-INF/services に置換、コンパイラプラグイン ID を変更（それ以外の処理は変えていない）
/*
 * Copyright 2018-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package me.tbsten.analysis.code.gen.testing.kct

import javax.tools.JavaCompiler
import javax.tools.ToolProvider

/**
 * ToolProvider has no synchronization internally, so if we don't synchronize from the outside we
 * could wind up loading the compiler classes multiple times from different class loaders.
 */
internal object SynchronizedToolProvider {
  val systemJavaCompiler: JavaCompiler
    get() {
      val compiler = synchronized(ToolProvider::class.java) { ToolProvider.getSystemJavaCompiler() }

      check(compiler != null) { "System java compiler is null! Are you running without JDK?" }
      return compiler
    }
}

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
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:common-util/src/main/kotlin/com/google/devtools/ksp/common/impl/PlatformInfoImpl.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更

package me.tbsten.analysis.code.gen.ksp.bridge.common.impl

import com.google.devtools.ksp.processing.JsPlatformInfo
import com.google.devtools.ksp.processing.JvmPlatformInfo
import com.google.devtools.ksp.processing.NativePlatformInfo
import com.google.devtools.ksp.processing.UnknownPlatformInfo

class JvmPlatformInfoImpl(
    override val platformName: String,
    override val jvmTarget: String,
    override val jvmDefaultMode: String
) : JvmPlatformInfo {
    override fun toString() = "$platformName ($jvmTarget)"
}

class JsPlatformInfoImpl(
    override val platformName: String
) : JsPlatformInfo {
    override fun toString() = platformName
}

class NativePlatformInfoImpl(
    override val platformName: String,
    override val targetName: String
) : NativePlatformInfo {
    override fun toString() = "$platformName ($targetName)"
}

class UnknownPlatformInfoImpl(
    override val platformName: String
) : UnknownPlatformInfo {
    override fun toString() = platformName
}

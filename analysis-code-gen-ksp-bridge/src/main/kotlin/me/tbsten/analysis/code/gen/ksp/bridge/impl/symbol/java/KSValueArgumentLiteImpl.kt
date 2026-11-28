/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/symbol/java/KSValueArgumentLiteImpl.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更
package me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.java

import me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin.AbstractKSValueArgumentImpl
import com.google.devtools.ksp.symbol.*

class KSValueArgumentLiteImpl(
    override val name: KSName?,
    override val value: Any?,
    override val parent: KSNode,
    override val origin: Origin,
    override val location: Location
) : AbstractKSValueArgumentImpl() {
    override val isSpread: Boolean = false

    override val annotations: Sequence<KSAnnotation> = emptySequence()
}

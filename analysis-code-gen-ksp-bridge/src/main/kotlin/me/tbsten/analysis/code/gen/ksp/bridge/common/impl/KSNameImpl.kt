/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:common-util/src/main/kotlin/com/google/devtools/ksp/common/impl/KSNameImpl.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更
package me.tbsten.analysis.code.gen.ksp.bridge.common.impl

import me.tbsten.analysis.code.gen.ksp.bridge.common.KSObjectCache
import com.google.devtools.ksp.symbol.KSName

class KSNameImpl private constructor(val name: String) : KSName {
    companion object : KSObjectCache<String, KSNameImpl>() {
        fun getCached(name: String) = cache.getOrPut(name) { KSNameImpl(name) }
    }

    override fun asString(): String {
        return name
    }

    override fun getQualifier(): String {
        val lastIndex = name.lastIndexOf('.')
        return if (lastIndex != -1) name.substring(0, lastIndex) else ""
    }

    override fun getShortName(): String {
        val lastIndex = name.lastIndexOf('.')
        return if (lastIndex != -1) name.substring(lastIndex + 1) else name
    }
}

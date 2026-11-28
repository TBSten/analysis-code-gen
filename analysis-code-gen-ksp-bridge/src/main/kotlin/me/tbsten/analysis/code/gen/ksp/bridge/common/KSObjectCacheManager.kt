/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:common-util/src/main/kotlin/com/google/devtools/ksp/common/KSObjectCacheManager.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更

package me.tbsten.analysis.code.gen.ksp.bridge.common

class KSObjectCacheManager {
    companion object {
        private val caches_prop = object : ThreadLocal<ArrayList<KSObjectCache<*, *>>>() {
            override fun initialValue(): ArrayList<KSObjectCache<*, *>> {
                return ArrayList()
            }
        }
        val caches
            get() = caches_prop.get()

        fun register(cache: KSObjectCache<*, *>) = caches.add(cache)
        fun clear() {
            caches.forEach { it.clear() }
            caches_prop.remove()
        }
    }
}

abstract class KSObjectCache<K, V> {
    private val cache_prop = ThreadLocal<MutableMap<K, V>>()

    val cache: MutableMap<K, V>
        get() {
            if (cache_prop.get() == null) {
                KSObjectCacheManager.register(this)
                cache_prop.set(mutableMapOf())
            }
            return cache_prop.get()
        }

    open fun clear() {
        cache.clear()
        cache_prop.remove()
    }
}

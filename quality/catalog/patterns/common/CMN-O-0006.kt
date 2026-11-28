// PATTERN: CMN-O-0006
// TITLE: 背景コード: 3 つの Gradle モジュール（api / impl / app）に分かれた普通のコード
// PROCESSORS: *
// EXPECT: ok
// ORIGIN: own
// STATUS: verified
// FILE: api/main/com/example/bg/modules/api/Repository.kt
package com.example.bg.modules.api

interface Repository {
    fun find(id: String): String?
}
// FILE: impl/main/com/example/bg/modules/impl/InMemoryRepository.kt
package com.example.bg.modules.impl

import com.example.bg.modules.api.Repository

internal class InMemoryRepository(private val data: Map<String, String> = emptyMap()) : Repository {
    override fun find(id: String): String? = data[id]
}

fun repository(): Repository = InMemoryRepository()
// FILE: app/main/com/example/bg/modules/app/Main.kt
package com.example.bg.modules.app

import com.example.bg.modules.impl.repository

fun main() {
    println(repository().find("a"))
}

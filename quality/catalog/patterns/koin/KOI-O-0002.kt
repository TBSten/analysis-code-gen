// PATTERN: KOI-O-0002
// TITLE: 実装クラスの上位インターフェースへ自動でバインドする（複数の上位型・ジェネリックな上位型）
// PROCESSORS: koin
// EXPECT: ok
// TAGS: koin.binding=auto-bind-interface
// ORIGIN: own
// STATUS: candidate
// FILE: main/com/example/autobind/Repository.kt
package com.example.autobind

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

interface Repository<T> {
    fun all(): List<T>
}

interface Closeable {
    fun close()
}

data class User(val id: Long, val name: String)

@Single
class UserRepository : Repository<User>, Closeable {
    override fun all(): List<User> = emptyList()
    override fun close() {}
}

@Single
class UserService(private val repository: Repository<User>)

@Module
@ComponentScan("com.example.autobind")
class AutoBindModule

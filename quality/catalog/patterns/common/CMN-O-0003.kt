// PATTERN: CMN-O-0003
// TITLE: 背景コード: fun interface・ローカルクラス・object 式・context parameters・suspend・関数型
// PROCESSORS: *
// EXPECT: ok
// ORIGIN: own
// STATUS: candidate
// NOTE: context parameters は Kotlin 2.2 以降の機能。2.4.0 以降を対象にするので常に使える
// FILE: main/com/example/bg/lang/Features.kt
package com.example.bg.lang

fun interface Mapper<in A, out B> {
    fun map(a: A): B
}

class Logger {
    fun log(message: String) = println(message)
}

context(logger: Logger)
fun audited(action: String) = logger.log(action)

suspend fun load(retry: Int = 3, onProgress: (Int) -> Unit = {}): List<String> = emptyList()

fun localThings(): Runnable {
    class Local(val x: Int)
    val anon = object : Runnable {
        override fun run() = println(Local(1).x)
    }
    return anon
}

val handler: suspend (String) -> Unit = {}

typealias StringMapper = Mapper<String, String>

@Deprecated("use Mapper", ReplaceWith("Mapper"))
fun interface OldMapper {
    fun map(a: String): String
}

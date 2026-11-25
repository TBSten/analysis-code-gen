package sample

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** 中間 SourceSet（jvmAndJsMain）と wasmJsMain で actual を書く */
expect fun platformName(): String

/** 固有 SourceSet で actual を書く */
expect class Clock() {
    fun now(): Long
}

fun greet(): String = "Hello, " + platformName()

internal fun commonInternal(): Int = 42

private fun commonPrivate(): Int = 1

fun numbers(): Flow<String> = flowOf(1, 2, 3).map { (it + commonPrivate()).toString() }

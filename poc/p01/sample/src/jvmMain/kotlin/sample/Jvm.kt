package sample

import java.io.File

actual class Clock actual constructor() {
    actual fun now(): Long = System.currentTimeMillis()
}

actual fun sharedPlatform(): String = "jvm"

fun jvmOnly(): String = File(".").name + useCommon() + sharedPlatform() + commonInternal()

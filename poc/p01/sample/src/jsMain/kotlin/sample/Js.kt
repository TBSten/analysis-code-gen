package sample

actual class Clock actual constructor() {
    actual fun now(): Long = kotlin.js.Date().getTime().toLong()
}

actual fun sharedPlatform(): String = "js"

fun jsOnly(): String = useCommon() + sharedPlatform() + webGreeting()

package sample

actual fun platformName(): String = "wasm"

actual class Clock actual constructor() {
    actual fun now(): Long = 0L
}

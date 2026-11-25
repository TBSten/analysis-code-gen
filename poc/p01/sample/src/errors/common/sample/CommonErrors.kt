package sample

import kotlinx.coroutines.Dispatchers

// 下位（中間）の API は共通から見えない
fun e1(): String = sharedPlatform()

// JVM の API は共通から見えない
fun e2(): String = java.io.File(".").name

// coroutines の JVM/Native 専用 API（Dispatchers.IO）は共通から見えない
fun e3(): Any = Dispatchers.IO

// 型の不一致
fun e4(): Int = greet()

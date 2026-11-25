package sample

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

actual fun platformName(): String = "jvm-or-js"

/** jvmMain / jsMain で actual を書く（中間で宣言する expect） */
expect fun sharedPlatform(): String

fun useCommon(): String = greet() + commonInternal() + Clock().now()

fun evenNumbers(): Flow<String> = numbers().filter { it.toInt() % 2 == 0 }

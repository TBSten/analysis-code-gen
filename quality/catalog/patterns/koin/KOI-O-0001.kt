// PATTERN: KOI-O-0001
// TITLE: @Module の無い定義を既定モジュールに集める
// PROCESSORS: koin
// EXPECT: ok
// OPTIONS: KOIN_DEFAULT_MODULE=true
// TAGS: koin.module=default-module
// ORIGIN: own
// STATUS: candidate
// FILE: main/com/example/defaultmodule/Services.kt
package com.example.defaultmodule

import org.koin.core.annotation.Factory
import org.koin.core.annotation.Single

@Single
class Clock

@Factory
class Greeter(private val clock: Clock) {
    fun greet(name: String): String = "hello $name"
}

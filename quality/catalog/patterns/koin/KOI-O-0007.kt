// PATTERN: KOI-O-0007
// TITLE: モジュールを Gradle モジュールに分けて includes でつなぐ
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: own
// STATUS: candidate
// NOTE: FILE の先頭が Gradle モジュール名。app は core に依存する
// FILE: core/main/com/example/core/CoreModule.kt
package com.example.core

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

interface Logger {
    fun log(message: String)
}

@Single
internal class ConsoleLogger : Logger {
    override fun log(message: String) = println(message)
}

@Module
@ComponentScan("com.example.core")
class CoreModule
// FILE: app/main/com/example/app/AppModule.kt
package com.example.app

import com.example.core.CoreModule
import com.example.core.Logger
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Factory
class Checkout(private val logger: Logger)

@Module(includes = [CoreModule::class])
@ComponentScan("com.example.app")
class AppModule

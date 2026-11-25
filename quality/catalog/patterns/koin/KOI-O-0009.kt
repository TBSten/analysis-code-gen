// PATTERN: KOI-O-0009
// TITLE: 独自 @Qualifier と @Named で同じ型を出し分ける
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: own
// STATUS: candidate
// FILE: main/com/example/qualifier/Dispatchers.kt
package com.example.qualifier

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Qualifier
import org.koin.core.annotation.Single

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

class Dispatcher(val name: String)

@Module
@ComponentScan
class DispatcherModule {
    @Single
    @IoDispatcher
    fun io(): Dispatcher = Dispatcher("io")

    @Single
    @Named("main")
    fun main(): Dispatcher = Dispatcher("main")

    @Single
    @Named(type = Dispatcher::class)
    fun typed(): Dispatcher = Dispatcher("typed")
}

@Single
class Downloader(@IoDispatcher private val io: Dispatcher, @Named("main") private val main: Dispatcher)

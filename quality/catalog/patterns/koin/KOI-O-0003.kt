// PATTERN: KOI-O-0003
// TITLE: nullable な依存・既定値つき引数・@InjectedParam の混在
// PROCESSORS: koin
// EXPECT: ok
// TAGS: koin.binding=nullable-param
// ORIGIN: own
// STATUS: verified
// FILE: main/com/example/nullable/Analytics.kt
package com.example.nullable

import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Module
import org.koin.core.annotation.ComponentScan

interface Tracker

@Factory
class Analytics(
    private val tracker: Tracker?,
    @InjectedParam private val screen: String,
    private val enabled: Boolean = true,
)

@Module
@ComponentScan
class NullableModule

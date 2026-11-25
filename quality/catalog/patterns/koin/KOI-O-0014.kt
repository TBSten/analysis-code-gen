// PATTERN: KOI-O-0014
// TITLE: 明示的な binds・Lazy・List による複数取得・createdAtStart
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: own
// STATUS: candidate
// FILE: main/com/example/binds/Plugins.kt
package com.example.binds

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

interface Plugin
interface Named

@Single(binds = [Plugin::class, Named::class])
class AuthPlugin : Plugin, Named

@Single(binds = [Plugin::class], createdAtStart = true)
class MetricsPlugin : Plugin

@Single
class PluginHost(private val plugins: List<Plugin>, private val auth: Lazy<AuthPlugin>)

@Module
@ComponentScan
class BindsModule

// PATTERN: KOI-O-0014
// TITLE: 明示的な binds・Lazy・List による複数取得・createdAtStart
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: own
// STATUS: rejected
// NOTE: K（KSP2 2.3.14）: K でビルドが失敗した: java.lang.RuntimeException: Exception while generating code for private final fun _get_com_example_binds_BindsModule_$lambda$0$2 ($this$single: org.koin.core.scope.Scope, <unused var>: org.koin.core.parameter.ParametersHolder): com.example.binds.PluginHost [companion] declared in org.koin.ksp.generated.BindsModuleGencom_example_bindsKt: FUN LOCAL_FUNCTION_FOR_LAMBDA name:_get_com_exam
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

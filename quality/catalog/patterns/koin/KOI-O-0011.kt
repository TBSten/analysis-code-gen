// PATTERN: KOI-O-0011
// TITLE: 名前つきスコープ・型スコープ・スコープの型（ViewModelScope / ActivityScope）
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: own
// STATUS: candidate
// FILE: androidMain/com/example/scope/Scopes.kt
package com.example.scope

import org.koin.core.annotation.ActivityScope
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped
import org.koin.core.annotation.ViewModelScope

class Session

@Scope(Session::class)
class SessionCache

@Scope(name = "checkout")
@Scoped
class Cart

@ViewModelScope
class ScreenState

@ActivityScope
class ActivityTracker

@Module
@ComponentScan
class ScopeModule

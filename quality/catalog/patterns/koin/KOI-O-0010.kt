// PATTERN: KOI-O-0010
// TITLE: @Property と @PropertyValue の既定値
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: own
// STATUS: verified
// FILE: main/com/example/property/Config.kt
package com.example.property

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Property
import org.koin.core.annotation.PropertyValue

@PropertyValue("server_url")
val defaultServerUrl = "https://example.com"

@Factory
class ApiClient(@Property("server_url") private val url: String, @Property("timeout") private val timeout: String)

@Module
@ComponentScan
class PropertyModule

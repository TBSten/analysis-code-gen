// PATTERN: KOI-O-0012
// TITLE: KMP: 共通の定義とプラットフォームごとの定義・expect/actual のモジュール
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: own
// STATUS: candidate
// FILE: shared/commonMain/com/example/kmp/PlatformModule.kt
package com.example.kmp

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

interface Storage

@Module
expect class PlatformModule()

@Single
class SettingsRepository(private val storage: Storage)
// FILE: shared/appleMain/com/example/kmp/AppleStorage.kt
package com.example.kmp

import org.koin.core.annotation.Single

@Single
class AppleStorage : Storage
// FILE: shared/iosMain/com/example/kmp/PlatformModule.ios.kt
package com.example.kmp

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan("com.example.kmp")
actual class PlatformModule actual constructor()
// FILE: shared/jvmMain/com/example/kmp/PlatformModule.jvm.kt
package com.example.kmp

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class PlatformModule actual constructor() {
    @Single
    fun storage(): Storage = object : Storage {}
}

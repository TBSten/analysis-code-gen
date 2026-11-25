// PATTERN: KOI-R-0039
// TITLE: darriousliu/PiPixiv / composeApp / IosAppModule
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: real:darriousliu/PiPixiv@1a2e76353cc3/composeApp/src/iosMain/kotlin/com/mrl/pixiv/IosAppModule.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: composeApp/iosMain/com/mrl/pixiv/IosAppModule.kt
package com.mrl.pixiv

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan
object IosAppModule

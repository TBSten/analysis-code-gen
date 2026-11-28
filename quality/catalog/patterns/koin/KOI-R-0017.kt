// PATTERN: KOI-R-0017
// TITLE: codymikol/git-down / root / ServicesModule
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: real:codymikol/git-down@dfbb1e11eab9/src/main/kotlin/com/codymikol/config/ServicesModule.kt
// LICENSE: MIT
// STATUS: verified
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので
// FILE: root/main/com/codymikol/config/ServicesModule.kt
package com.codymikol.config

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan("com.codymikol.services")
class ServicesModule

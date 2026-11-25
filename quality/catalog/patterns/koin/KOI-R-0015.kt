// PATTERN: KOI-R-0015
// TITLE: codymikol/git-down / root / BeansModule
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: real:codymikol/git-down@dfbb1e11eab9/src/main/kotlin/com/codymikol/config/BeansModule.kt
// LICENSE: MIT
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: root/main/com/codymikol/config/BeansModule.kt
package com.codymikol.config

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan("com.codymikol.beans")
class BeansModule

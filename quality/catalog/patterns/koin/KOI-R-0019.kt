// PATTERN: KOI-R-0019
// TITLE: codymikol/git-down / root / HighlightingModule
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: real:codymikol/git-down@dfbb1e11eab9/src/main/kotlin/com/codymikol/config/HighlightingModule.kt
// LICENSE: MIT
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: root/main/com/codymikol/config/HighlightingModule.kt
package com.codymikol.config

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan("com.codymikol.highlighting")
class HighlightingModule

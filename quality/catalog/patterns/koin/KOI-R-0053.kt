// PATTERN: KOI-R-0053
// TITLE: MythicDrops/MythicDrops / spigot / MythicDropsDevModule
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: real:MythicDrops/MythicDrops@b4ee6c605f64/spigot/src/main/kotlin/dev/mythicdrops/MythicDropsDevModule.kt
// LICENSE: MIT
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: spigot/main/dev/mythicdrops/MythicDropsDevModule.kt
package dev.mythicdrops

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@ComponentScan
@Module
internal class MythicDropsDevModule

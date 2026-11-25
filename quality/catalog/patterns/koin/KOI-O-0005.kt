// PATTERN: KOI-O-0005
// TITLE: 生成先パッケージを変える
// PROCESSORS: koin
// EXPECT: ok
// OPTIONS: KOIN_GENERATION_PACKAGE=com.example.generated.di
// ORIGIN: own
// STATUS: candidate
// FILE: main/com/example/genpackage/Module.kt
package com.example.genpackage

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Single
class Config(val name: String = "app")

@Module
@ComponentScan
class GenPackageModule

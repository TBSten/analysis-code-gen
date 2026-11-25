// PATTERN: KOI-O-0004
// TITLE: コンパイル時チェックで定義漏れをエラーにする
// PROCESSORS: koin
// EXPECT: error
// EXPECT_MESSAGE: Missing definition for 'api : com.example.configcheck.Api'
// OPTIONS: KOIN_CONFIG_CHECK=true
// TAGS: koin.other=config-check-error
// ORIGIN: own
// STATUS: candidate
// FILE: main/com/example/configcheck/App.kt
package com.example.configcheck

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

interface Api

@Single
class Repository(private val api: Api)

@Module
@ComponentScan
class AppModule

// PATTERN: KOI-T-0003
// TITLE: examples / android-library-other
// PROCESSORS: koin
// EXPECT: ok
// OPTIONS: KOIN_CONFIG_CHECK=true, KOIN_LOG_TIMES=true
// ORIGIN: test:InsertKoinIO/koin-annotations@1d4c1af21faa/examples/android-library-other
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: サンプルのモジュール丸ごと（テストソースは除く）
// FILE: androidMain/org/koin/sample/androidx/multi/LibFooB.kt
package org.koin.sample.androidx.multi

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.example.multi.FooBase

@Module
@Configuration("default","lib")
@ComponentScan
class LibFooBModule

@Factory
class FooB : FooBase() {
    val text = "text B"
}

@Factory
class FooC : FooBase() {
    val text = "text C"
}

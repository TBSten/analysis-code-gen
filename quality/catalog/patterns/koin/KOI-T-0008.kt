// PATTERN: KOI-T-0008
// TITLE: examples / cycle-module
// PROCESSORS: koin
// EXPECT: ok
// OPTIONS: KOIN_CONFIG_CHECK=true, KOIN_DEFAULT_MODULE=true, KOIN_LOG_TIMES=true
// TAGS: koin.other=cycle
// ORIGIN: test:InsertKoinIO/koin-annotations@1d4c1af21faa/examples/cycle-module
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: サンプルのモジュール丸ごと（テストソースは除く）
// FILE: main/org/koin/example/cycle/Cycle.kt
package org.koin.example.cycle

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class CycleModule

@Single
public class CycleA(private val b : CycleB)
@Single
public class CycleB(private val c : CycleC)
@Single
public class CycleC(private val a : CycleA)

@Single
public class CycleLazyA(private val b : Lazy<CycleLazyB>)
@Single
public class CycleLazyB(private val a : Lazy<CycleLazyA>)
// FILE: main/org/koin/example/cycle/LazyCycle.kt
package org.koin.example.cycle

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

public class LazyCycleA(public val b : LazyCycleB)
public class LazyCycleB(public val a : LazyCycleA)


@Module
public class LazyCycleModule {

    @Single
    public fun providesLazyCycleA(b : LazyCycleB): LazyCycleA = LazyCycleA(b)

    @Single
    public fun providesLazyCycleB(a : LazyCycleA): LazyCycleB = LazyCycleB(a)
}

// PATTERN: KOI-T-0002
// TITLE: examples / android-library
// PROCESSORS: koin
// EXPECT: ok
// OPTIONS: KOIN_CONFIG_CHECK=true, KOIN_LOG_TIMES=true
// ORIGIN: test:InsertKoinIO/koin-annotations@1d4c1af21faa/examples/android-library
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: サンプルのモジュール丸ごと（テストソースは除く）
// FILE: androidMain/it/example/component/ExampleModule.kt
package it.example.component

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan
class ExampleModule
// FILE: androidMain/it/example/component/singleton/Example.kt
package it.example.component.singleton

import org.koin.core.annotation.Single

@Single
class ExampleSingleton
// FILE: androidMain/org/koin/sample/android/library/CommonModule.kt
package org.koin.sample.android.library

import it.example.component.ExampleModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [ExampleModule::class])
@ComponentScan
class CommonModule
// FILE: androidMain/org/koin/sample/android/library/CommonRepository.kt
package org.koin.sample.android.library

import org.koin.core.annotation.Single

interface AbstractRepository {
    fun getId() : String
}

@Single
class OtherParam

@Single
class CommonRepository(val lazyParam: Lazy<OtherParam>) : AbstractRepository{
    override fun getId(): String = "_ID_"
}
// FILE: androidMain/org/koin/sample/android/library/MyScope.kt
package org.koin.sample.android.library

class MyScope {
}
// FILE: androidMain/org/koin/sample/android/library/TestAndroidComponent.kt
package org.koin.sample.android.library

import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

interface Tester

@Single
@Named("fake_tester")
class FakeTester : Tester

@Single
@Named("other_tester")
class OtherTester : Tester
// FILE: androidMain/org/koin/sample/androidx/data/DataConsumer.kt
package org.koin.sample.androidx.data

import org.koin.core.annotation.Factory
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped
import java.util.UUID

@Factory
class DataConsumer

class MyDataConsumer(val dc : DataConsumer)

@Factory
fun funDataConsumer(dc : DataConsumer) = MyDataConsumer(dc)

class MyDataScope

@Scoped
@Scope(MyDataScope::class)
class ScopeData {
    val id : String = UUID.randomUUID().toString()
}
// FILE: androidMain/org/koin/sample/clients/ClientModule.kt
package org.koin.sample.clients

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module(includes = [ClientModuleA::class, ClientModuleB::class])
class ClientModule

@Module
class ClientModuleA {

    @Single
    @Named("clientA")
    fun createClient() = HttpClient(CIO) {}
}

@Module
class ClientModuleB {

    @Single
    @Named("clientB")
    fun createClient() = HttpClient(CIO) {}
}
// FILE: androidMain/org/koin/sample/multi/LibFooA.kt
package org.koin.sample.multi

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.example.multi.FooBase

@Module
@Configuration("default","lib")
@ComponentScan
class LibFooAModule

@Factory
class FooA: FooBase(){
    val text  = "text A"
}

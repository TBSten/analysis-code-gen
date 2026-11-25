// PATTERN: KOI-T-0006
// TITLE: examples / coffee-maker-module
// PROCESSORS: koin
// EXPECT: ok
// OPTIONS: KOIN_CONFIG_CHECK=true, KOIN_LOG_TIMES=true
// ORIGIN: test:InsertKoinIO/koin-annotations@1d4c1af21faa/examples/coffee-maker-module
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: サンプルのモジュール丸ごと（テストソースは除く）
// FILE: main/org/koin/example/coffee/DetachedComponents.kt
package org.koin.example.coffee

import org.koin.core.annotation.Single

@Single
class MyDetachCoffeeComponent
// FILE: main/org/koin/example/multi/FooBase.kt
package org.koin.example.multi

open class FooBase {

    val textBase = "text base"
}
// FILE: main/org/koin/example/test/ext/ExternalModule.kt
package org.koin.example.test.ext

import jakarta.inject.Singleton
import org.koin.core.annotation.*
import org.koin.example.test.include.IncludedModule


class TestComponent
class TestComponentConsumer(val tc: TestComponent, val id: String)

@Single
class TestComponentConsumer2(@Named("tc") val tc: TestComponent, @InjectedParam val id: String)

@Factory
class TestComponentConsumer3(val tc: TestComponent?, @InjectedParam val id: String?)

@Single
class PropertyComponent(@Property("prop_id") val id: String)

class PropertyComponent2(val id: String)

@Module(includes = [IncludedModule::class])
@Configuration
@ComponentScan
class ExternalModule {

    @Single(createdAtStart = true)
    @Named("tc")
    fun testComponent() = TestComponent()

    @Singleton
    fun testComponentConsumer(@Named("tc") tc: TestComponent, @InjectedParam id: String) = TestComponentConsumer(tc, id)

    @Single
    fun propertyComponent2(@Property("prop_id") id: String) = PropertyComponent2(id)
}
// FILE: main/org/koin/example/test/ext2/ExternalModule.kt
package org.koin.example.test.ext2

import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single


class MyExt2Component()

@Configuration
@Module
class ExternalModule {

    @Single
    fun myExt2Component() = MyExt2Component()
}
// FILE: main/org/koin/example/test/include/IncludedModule.kt
package org.koin.example.test.include

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

class IncludedComponent

@Module
class IncludedModule {

    @Single
    fun includedComponent() = IncludedComponent()
}
// FILE: main/org/koin/example/test/scope/ScopeModule.kt
package org.koin.example.test.scope

import org.koin.core.annotation.*

class MyScope
interface AdditionalTypeScope
interface AdditionalTypeScope2

const val MY_SCOPE_SESSION = "MY_SCOPE_SESSION"

@Scope(MyScope::class)
class MyScopedComponent(val myScope: MyScope)

class MyScopedComponent2(val myScope: MyScope)

@Scope(MyScope::class)
@Factory
class MyScopedComponent3(val myScope: MyScope) : AdditionalTypeScope

@Scope(MyScope::class)
@Scoped(binds = [AdditionalTypeScope2::class])
class MyScopedComponent5(val myScope: MyScope) : AdditionalTypeScope, AdditionalTypeScope2

class MyScopedComponent4(val myScope: MyScope)

@Scope(name = MY_SCOPE_SESSION)
class MyScopedSessionComponent

@Module
@Configuration
@ComponentScan
class ScopeModule {

    @Scope(MyScope::class)
    fun myScopedComponent2(myScope: MyScope) = MyScopedComponent2(myScope)

    @Scope(MyScope::class)
    @Factory
    fun myScopedComponent4(myScope: MyScope) = MyScopedComponent4(myScope)
}

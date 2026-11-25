// PATTERN: KOI-T-0009
// TITLE: examples / other-ksp
// PROCESSORS: koin
// EXPECT: ok
// OPTIONS: KOIN_CONFIG_CHECK=true, KOIN_DEFAULT_MODULE=true, KOIN_LOG_TIMES=true
// ORIGIN: test:InsertKoinIO/koin-annotations@1d4c1af21faa/examples/other-ksp
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: サンプルのモジュール丸ごと（テストソースは除く）
// FILE: main/org/koin/example/MainApp.kt
package org.koin.example

// be sure to import "import org.koin.ksp.generated.*"
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.example.`interface`.MyInterfaceExt
import org.koin.example.newmodule.MyModule2
import org.koin.example.newmodule.mymodule.MyModule3
import org.koin.ksp.generated.*

@Module
@Configuration
@ComponentScan
class OtherModule

@Single
public class MyComponent : MyInterfaceExt

public class MyOtherComponent(public val i: MyInterfaceExt)

@Single
public fun createMyOtherComponent(i: MyInterfaceExt) : MyOtherComponent= MyOtherComponent(i)

@KoinApplication
object OtherApp

public fun main() {

    OtherApp.startKoin {
        printLogger()
    }
}
// FILE: main/org/koin/example/animal/Animal.kt
package org.koin.example.animal

import jakarta.inject.Qualifier
import org.koin.core.annotation.*
import kotlin.random.Random

public interface Animal

@Single
public class Dog : Animal

@Single
public class Cat : Animal

public class Bunny(public val color: String) : Animal

@Single
public class Farm(@WhiteBunny public val whiteBunny: Bunny, @BlackBunny public val blackBunny: Bunny)

@Named
public annotation class WhiteBunny

@Qualifier
public annotation class BlackBunny

@Module
@Configuration
@ComponentScan
public class AnimalModule {
    @Factory
    public fun animal(cat: Cat, dog: Dog): Animal = if (randomBoolean()) cat else dog

    @Single
    @WhiteBunny
    public fun whiteBunny(): Bunny = Bunny("White")

    @Single
    @BlackBunny
    public fun blackBunny(): Bunny = Bunny("Black")

    private fun randomBoolean(): Boolean = Random.nextBoolean()
}
// FILE: main/org/koin/example/binds/BindTests.kt
package org.koin.example.binds

import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

public interface Network
public class NetworkImpl : Network

public interface Call
public class CallImpl : Call

public class ClientWithBinds(public val call : Call, public val network: Network)

@Module
@Configuration
public class BindTestsModule{

    @Single(binds = [Call::class])
    public fun call(): CallImpl = CallImpl()

    @Single(binds = [Network::class])
    public fun network(): NetworkImpl = NetworkImpl()

    @Single
    public fun client(call: Call, network: Network) : ClientWithBinds = ClientWithBinds(call,network)
}
// FILE: main/org/koin/example/by/example/ByExampleSingle.kt
package org.koin.example.by.example

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan
public class ByModule

@Single
public class ByExampleSingle
// FILE: main/org/koin/example/defaultparam/DefaultParam.kt
package org.koin.example.defaultparam

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Module

@Module
@Configuration
@ComponentScan
public class MyModule

public const val COMPONENT_DEFAULT: String = "default"

@Factory
public class Component(@InjectedParam public val param: String = COMPONENT_DEFAULT)
// FILE: main/org/koin/example/empty/EmptyModule.kt
package org.koin.example.empty

import org.koin.core.annotation.Module

@Module
public class EmptyModule {
}
// FILE: main/org/koin/example/inject/MyInjectedCtorClass.kt
package org.koin.example.inject

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

@Module
@ComponentScan
@Configuration
class MyCompatModule

@Singleton
class MySingleton

class MyInjectedCtorClass @Inject constructor(val s : MySingleton)
// FILE: main/org/koin/example/injparam/InjectedParamModule.kt
package org.koin.example.injparam

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Module

@Module
@Configuration
@ComponentScan
public class InjectedParamModule

@Factory
public class MyInjectFactory(@InjectedParam public val ints: List<Int>)
// FILE: main/org/koin/example/interface/MyInterfaceExt.kt
package org.koin.example.`interface`

public interface MyInterfaceExt {
}
// FILE: main/org/koin/example/newmodule/ComponentWithDefaultValues.kt
package org.koin.example.newmodule

import org.koin.core.annotation.Factory
import org.koin.core.annotation.Property
import org.koin.core.annotation.PropertyValue
import org.koin.core.annotation.Single

public interface ComponentInterface {

    public companion object Default : ComponentInterface
}

@Single
public class ComponentWithDefaultValues(
    private val dependency: ComponentInterface = ComponentInterface.Default
)

@Factory
public class ComponentWithProps(
    @Property("id") public val id : String
){
    public companion object {
        @PropertyValue("id")
        public const val DEFAULT_ID : String = "_empty_id"
    }
}
// FILE: main/org/koin/example/newmodule/MyModule2.kt
package org.koin.example.newmodule

import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.example.`interface`.MyInterfaceExt

public class MyOtherComponent2(public val i: MyInterfaceExt)
public class MyOtherComponent3F(public val c2: MyOtherComponent2)

@Module
@Configuration
public class MyModule2 {

    @Single
    public fun myOtherComponent2(i: MyInterfaceExt): MyOtherComponent2 = MyOtherComponent2(i)

    @Single
    public fun myOtherComponent3F(c2: MyOtherComponent2): MyOtherComponent3F = MyOtherComponent3F(c2)
}
// FILE: main/org/koin/example/newmodule/mymodule/MyModule.kt
package org.koin.example.newmodule.mymodule

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.example.`interface`.MyInterfaceExt

@Single(createdAtStart = true)
public class MyOtherComponent3(public val i : MyInterfaceExt)

@Module
@Configuration
@ComponentScan
public class MyModule3
// FILE: main/org/koin/example/qualifier/CustomQualifierComponent.kt
package org.koin.example.qualifier

import jakarta.inject.Qualifier
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import kotlin.annotation.AnnotationRetention.RUNTIME

@Qualifier
@Retention(RUNTIME)
annotation class Dispatcher(val niaDispatcher: NiaDispatchers)

enum class NiaDispatchers {
    Default,
    IO,
}

data class CoroutineDispatcher(val name : String)
class CoroutineDispatcherConsumer(val dispatcher : CoroutineDispatcher)

@Module
@Configuration
class DispatchersModule {

    @Single
    @Dispatcher(NiaDispatchers.IO)
    fun providesIODispatcher(): CoroutineDispatcher = CoroutineDispatcher("IO")

    @Single
    @Dispatcher(NiaDispatchers.Default)
    fun providesDefaultDispatcher(): CoroutineDispatcher = CoroutineDispatcher("Default")

    @Single
    fun coroutineDispatcherConsumer(@Dispatcher(NiaDispatchers.IO) dispatcher: CoroutineDispatcher) = CoroutineDispatcherConsumer(dispatcher)
}
// FILE: main/org/koin/example/qualifier/QualifierComponents.kt
package org.koin.example.qualifier

import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

public class LazyStuffCounter(public val lazyCounter : Lazy<StuffCounter>)
public data class StuffList(public val list : List<Stuff>)
public class StuffCounter(public val list: List<Stuff>, public val name : String = "")
public data class Stuff(val name : String)

@Module
@Configuration
public class QualifierModule {

    //Can't have Qualifier + List

    @Named("stuffs")
    @Single
    public fun listOfStuff() : StuffList = StuffList(listOf(Stuff("1"),Stuff("2")))

    @Single
    public fun counter(@Named("stuffs") stuffs : StuffList): StuffCounter = StuffCounter(stuffs.list)

    @Single
    @Named("lazyStuffCounter")
    public fun counter2(@Named("stuffs") stuffs : StuffList): StuffCounter = StuffCounter(stuffs.list,"lazy")

//    @Single
//    public fun lazyCounter(@Named("lazyStuffCounter") counter : StuffCounter) : LazyStuffCounter = LazyStuffCounter(lazy { counter })
     @Single
     public fun lazyCounter(@Named("lazyStuffCounter") counter : Lazy<StuffCounter>) : LazyStuffCounter = LazyStuffCounter(counter)

    @Named("another-counter")
    @Single
    public fun anotherList(): StuffList = StuffList(listOf(Stuff("1"),Stuff("2")))

    @Single
    @Named("another-counter")
    public fun anotherCounter(@Named("another-counter") stuffs: StuffList): StuffCounter = StuffCounter(stuffs.list,"another-counter")
}
// FILE: main/org/koin/example/scope/ScopeModule.kt
package org.koin.example.scope

import org.koin.core.annotation.*
import org.koin.example.MyOtherComponent

@Scope(name = "my_scope")
public class MyScope

@Scope(name = "my_scope")
@Scoped
public class MyScopedInstance

@Scope(name = "my_scope")
@Factory
public class MyScopeFactory(
    public val oc : MyOtherComponent,
    @ScopeId(name = "my_scope_id")
    public val msi : MyScopedInstance
)

@Module
@Configuration
@ComponentScan
public class ScopeModule
// FILE: main/org/koin/example/supertype/Types.kt
package org.koin.example.supertype

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan
public class SuperTypesModule

public interface A

public open class B : A

public interface D

@Single
public class C : B(),D


class MyType {

    @Single
    class MyChildType
}

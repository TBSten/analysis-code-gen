// PATTERN: KOI-T-0004
// TITLE: examples / coffee-maker
// PROCESSORS: koin
// EXPECT: ok
// OPTIONS: KOIN_CONFIG_CHECK=true, KOIN_LOG_TIMES=true
// ORIGIN: test:InsertKoinIO/koin-annotations@1d4c1af21faa/examples/coffee-maker
// LICENSE: Apache-2.0
// STATUS: rejected
// NOTE: サンプルのモジュール丸ごと（テストソースは除く） K（KSP2 2.3.14）: K でビルドが失敗した: --> Missing definition for 'coffeeMaker : org.koin.example.coffee.CoffeeMaker' in 'org.koin.example.di.CoffeeMakerTesterTest'. Fix by adding a definition annotation or function definition in module class.
// FILE: main/RootLevel.kt
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import java.util.UUID

@Module
class RootLevelModule {

    @Single
    fun declareRootLevelData() : RootLevelData = RootLevelData()
}

class RootLevelData() {
    val id : String = UUID.randomUUID().toString()
}
// FILE: main/org/koin/example/CoffeeApp.kt
package org.koin.example

import org.koin.core.annotation.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.example.coffee.CoffeeMaker
import org.koin.example.di.CoffeeAppModule
import org.koin.example.di.CoffeeTesterModule
import org.koin.example.tea.TeaModule
import org.koin.example.test.ext.ExternalModule
import org.koin.example.test.scope.ScopeModule
import org.koin.ksp.generated.*
import kotlin.time.measureTime

class CoffeeApp : KoinComponent {
    val maker: CoffeeMaker by inject()
}

// be sure to import "import org.koin.ksp.generated.*"

@KoinApplication
object CoffeeKoinApp

fun main() {

//    startKoin {
//        printLogger(Level.DEBUG)
//        // if no module
////        defaultModule()
//
//        // else let's use our modules
//        modules(
//            CoffeeAppModule().module,
//            CoffeeTesterModule().module,
//            TeaModule().module,
//
//            ExternalModule().module,
//            org.koin.example.test.ext2.ExternalModule().module,
//            ScopeModule().module
//        )
//    }

    CoffeeKoinApp.startKoin {
        printLogger(Level.DEBUG)
    }

    val coffeeShop = CoffeeApp()
    val t = measureTime {
        coffeeShop.maker.brew()
    }
    println("Got Coffee in $t")
}
// FILE: main/org/koin/example/coffee/CoffeeMaker.kt
package org.koin.example.coffee

import jakarta.inject.Singleton
import org.koin.example.coffee.pump.Pump

@Singleton
class CoffeeMaker(private val pump: Pump, private val heater: Heater) {

    fun brew() {
        heater.on()
        pump.pump()
        println(" [_]P coffee! [_]P ")
        heater.off()
    }
}
// FILE: main/org/koin/example/coffee/CoffeePumpList.kt
package org.koin.example.coffee

import org.koin.core.annotation.Single
import org.koin.example.coffee.pump.Pump

@Single
class CoffeePumpList(val list : List<Pump>)
// FILE: main/org/koin/example/coffee/ElectricHeater.kt
package org.koin.example.coffee

import org.koin.core.annotation.Single

@Single
class ElectricHeater : Heater {

    private var heating: Boolean = false

    override fun on() {
        println("~ ~ ~ heating ~ ~ ~")
        heating = true
    }

    override fun off() {
        heating = false
    }

    override fun isHot(): Boolean = heating
}
// FILE: main/org/koin/example/coffee/Heater.kt
package org.koin.example.coffee

interface Heater {
    fun on()
    fun off()
    fun isHot(): Boolean
}
// FILE: main/org/koin/example/coffee/pump/FakePump.kt
package org.koin.example.coffee.pump

import org.koin.core.annotation.Single

@Single
class FakePump : Pump {
    override fun pump() {
        println("fake pump")
    }
}
// FILE: main/org/koin/example/coffee/pump/Pump.kt
package org.koin.example.coffee.pump

interface Pump {
    fun pump()
}
// FILE: main/org/koin/example/coffee/pump/PumpCounter.kt
package org.koin.example.coffee.pump

class PumpCounter(val pump : List<Pump>){
    val count = pump.size
}
// FILE: main/org/koin/example/coffee/pump/Thermosiphon.kt
package org.koin.example.coffee.pump

import org.koin.core.annotation.Single
import org.koin.example.coffee.Heater

@Single
class Thermosiphon(private val heater: Heater) : Pump {
    override fun pump() {
        if (heater.isHot()) {
            println("=> => pumping => =>")
        }
    }
}
// FILE: main/org/koin/example/di/CoffeeAppModule.kt
package org.koin.example.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.example.coffee.CoffeeMaker
import org.koin.example.coffee.pump.Pump
import org.koin.example.coffee.pump.PumpCounter
import org.koin.example.test.CoffeeMakerTesterTest
import org.koin.example.test.CoffeeMakerTesterTestImpl

@Module
@Configuration
@ComponentScan("org.koin.example.coffee")
class CoffeeAppModule {

    @Single
    fun pumpCounter(list : List<Pump>) = PumpCounter(list)
}

@Module
@Configuration
@ComponentScan(value = ["org.koin.example.test"])
class CoffeeTesterModule {

    @Single
    fun CoffeeMakerTesterTest(coffeeMaker: CoffeeMaker) : CoffeeMakerTesterTest = CoffeeMakerTesterTestImpl(coffeeMaker)
}
// FILE: main/org/koin/example/tea/TeaModule.kt
package org.koin.example.tea

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

@Module
@Configuration
@ComponentScan
class TeaModule
// FILE: main/org/koin/example/tea/TeaPot.kt
package org.koin.example.tea

import org.koin.core.annotation.Single
import org.koin.example.coffee.Heater

@Single
class TeaPot(val heater: Heater)
// FILE: main/org/koin/example/test/CoffeeMakerTester.kt
package org.koin.example.test

import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.koin.example.coffee.CoffeeMaker

@Factory
@Named("test")
class CoffeeMakerTester(val coffeeMaker: CoffeeMaker)

interface CoffeeMakerTesterTest {
    fun coffeeTest()
}

class CoffeeMakerTesterTestImpl(val coffeeMaker: CoffeeMaker) : CoffeeMakerTesterTest {
    override fun coffeeTest() {
        coffeeMaker.brew()
    }
}

// PATTERN: KOI-T-0001
// TITLE: examples / android-coffee-maker
// PROCESSORS: koin
// EXPECT: ok
// OPTIONS: KOIN_CONFIG_CHECK=true, KOIN_DEFAULT_MODULE=true, KOIN_LOG_TIMES=true
// ORIGIN: test:InsertKoinIO/koin-annotations@1d4c1af21faa/examples/android-coffee-maker
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: サンプルのモジュール丸ごと（テストソースは除く）
// FILE: androidMain/org/koin/sample/androidx/MainActivity.kt
package org.koin.sample.androidx

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.activityRetainedScope
import org.koin.androidx.scope.activityScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.sample.android.library.MyScope
import org.koin.sample.androidx.app.*
import org.koin.sample.androidx.app.scope.MyActivityOtherScope
import org.koin.sample.androidx.app.scope.MyActivityScope
import org.koin.sample.androidx.app.scope.ScopeViewModel
import org.koin.sample.androidx.data.TaskDatasource
import org.koin.sample.androidx.di.UseContext
import org.koin.sample.androidx.multi.FooB
import org.koin.sample.multi.FooA

class MainActivity : AppCompatActivity(), AndroidScopeComponent {

    override val scope: Scope by activityScope()

    // inject & ViewModel
    val coffeeViewModel : CoffeeViewModel by viewModel()
    val myPresenter : MyPresenter by inject { parametersOf(this@MainActivity) }
    val todoViewModel : TodoViewModel by viewModel()
    val heater : AndroidHeater by inject()
    val coffeeFactory : AndroidCoffeeMakerTester by inject()
    val scopeVM : ScopeViewModel by viewModel()

    val fooA : FooA by inject()
    val fooB : FooB by inject()

    val myActivityScope : MyActivityScope by inject()
    val myOtherActivityScope : MyActivityOtherScope by inject()

    private val button : Button by lazy { findViewById(R.id.main_button) }
    private val textView : TextView by lazy { findViewById(R.id.main_text) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getKoin().declare(MyProvidedComponent())

        setContentView(R.layout.main_activity)
        title = "Android Coffee Maker"

        button.setOnClickListener {
            textView.text = "Coffee Inside !"
        }

        assert(coffeeViewModel.repository.getId() == "_ID_")

        assert(myPresenter.mainActivity == this)

        assert(todoViewModel.repository.local == getKoin().get<TaskDatasource>(named("local")))
        assert(todoViewModel.repository.remote == getKoin().get<TaskDatasource>(named("remote")))
        println("resolved: $heater - $coffeeFactory")

        val scope = getKoin().createScope<MyScope>()
        scope.get<ScopedStuff>()

        println("VM scope data: ${scopeVM.sd.id}")
        println("VM scope other data: ${scopeVM.sod.id}")
        println("VM scope 2nd data: ${scopeVM.ssd.id}")

        println("Activity Scope Data: ${myActivityScope.id}")
        println("Activity Other Scope Data: ${myOtherActivityScope.id}")

        assert(fooB.text != fooA.text)
        assert(fooB.textBase == fooA.textBase)

        getKoin().get<UseContext>()
    }
}
// FILE: androidMain/org/koin/sample/androidx/MainApplication.kt
package org.koin.sample.androidx

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level
import org.koin.core.option.viewModelScopeFactory
import org.koin.ksp.generated.startKoin
import org.koin.sample.androidx.di.MyKoinApp

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        MyKoinApp.startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@MainApplication)
            options(
                viewModelScopeFactory()
            )
        }

//        startKoin {
//            androidLogger(Level.DEBUG)
//            androidContext(this@MainApplication)
//            modules(
////                defaultModule,
//                AppModule().module,
//            )
//            options(
//                viewModelScopeFactory()
//            )
//        }
    }
}
// FILE: androidMain/org/koin/sample/androidx/app/AndroidCoffeeMakerTester.kt
package org.koin.sample.androidx.app

import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named
import org.koin.sample.android.library.Tester

@Factory
class AndroidCoffeeMakerTester(@Named("fake_tester") val tester: Tester)
// FILE: androidMain/org/koin/sample/androidx/app/AndroidHeater.kt
package org.koin.sample.androidx.app

import android.util.Log
import org.koin.core.annotation.Single

@Single
class AndroidHeater {

    val tag = this::class.simpleName

    private var isHot = false

    fun on() {
        isHot = true
        Log.i(tag, " - heater on -")
    }

    fun off() {
        isHot = false
        Log.i(tag, " - heater off -")
    }

    fun isHot(): Boolean {
        Log.i(tag, " - heater isHot? $isHot -")
        return isHot
    }
}
// FILE: androidMain/org/koin/sample/androidx/app/CoffeeViewModel.kt
package org.koin.sample.androidx.app

import androidx.lifecycle.ViewModel
import org.koin.android.annotation.KoinViewModel
import org.koin.sample.android.library.AbstractRepository
import org.koin.sample.androidx.repository.Repository


@KoinViewModel
class CoffeeViewModel(val repository : AbstractRepository) : ViewModel()

@KoinViewModel
class TodoViewModel(val repository: Repository) : ViewModel()
// FILE: androidMain/org/koin/sample/androidx/app/MyPresenter.kt
package org.koin.sample.androidx.app

import android.content.Context
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Provided
import org.koin.sample.androidx.MainActivity
import org.koin.sample.androidx.data.ProvidedComponent

class MyProvidedComponent

@Factory
class MyPresenter(@InjectedParam val mainActivity: MainActivity, val context: Context, @Provided val provided : MyProvidedComponent)

@Factory
class MyPresenterHolder(val mp : MyPresenter)
// FILE: androidMain/org/koin/sample/androidx/app/ScopedStuff.kt
package org.koin.sample.androidx.app

import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped
import org.koin.sample.android.library.MyScope

@Scoped
@Scope(MyScope::class)
class ScopedStuff
// FILE: androidMain/org/koin/sample/androidx/app/scope/ScopeComponents.kt
package org.koin.sample.androidx.app.scope

import android.app.Activity
import androidx.lifecycle.ViewModel
import org.koin.android.annotation.ActivityScope
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.ViewModelScope
import java.util.UUID

@ViewModelScope
class ScopedData {
    val id = UUID.randomUUID().toString()
}

@ViewModelScope
class ScopedOtherData {
    val id = UUID.randomUUID().toString()
}

@ViewModelScope
class SecondOtherData(val sd : ScopedData) {
    val id = sd.id
}

@KoinViewModel
class ScopeViewModel(val sd : ScopedData, val sod : ScopedOtherData, val ssd : SecondOtherData) : ViewModel()

@ActivityScope
class MyActivityScope {
    val id = UUID.randomUUID().toString()
}

class MyActivityInjectedScope(activity: Activity) {
    val id = activity.hashCode()
}

@ActivityScope
class MyActivityOtherScope(val mas : MyActivityScope) {
    val id = mas.id
}
// FILE: androidMain/org/koin/sample/androidx/data/Datasources.kt
package org.koin.sample.androidx.data

import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

interface TaskDatasource

@Single
@Named("local")
class LocalDatasource : TaskDatasource

@Single
@Named("remote")
class RemoteDatasource : TaskDatasource
// FILE: androidMain/org/koin/sample/androidx/data/ProvidedComponent.kt
package org.koin.sample.androidx.data

class ProvidedComponent
// FILE: androidMain/org/koin/sample/androidx/di/AppModules.kt
package org.koin.sample.androidx.di

import android.app.Activity
import android.content.Context
import org.koin.android.annotation.ActivityScope
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Factory
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided
import org.koin.core.scope.Scope
import org.koin.sample.android.library.CommonModule
import org.koin.sample.androidx.app.scope.MyActivityInjectedScope
import org.koin.sample.androidx.multi.LibFooConsumerModule
import org.koin.sample.androidx.repository.RepositoryModule
import org.koin.sample.clients.ClientModule

class UseContext(val context: Context)

//@KoinApplication(
//    configurations = ["default","lib"]
//)

@KoinApplication
object MyKoinApp

@Configuration
@Module(includes = [DataModule::class, LibFooConsumerModule::class])
@ComponentScan("org.koin.sample.androidx.app")
class AppModule {

    @Factory
    fun useKoinScope(scope : Scope) : UseContext = UseContext(scope.get())

    @ActivityScope
    fun createOnActivityScope(@Provided activity : Activity) : MyActivityInjectedScope = MyActivityInjectedScope(activity)
}

@Module(includes = [CommonModule::class, ClientModule::class, RepositoryModule::class])
@ComponentScan("org.koin.sample.androidx.data")
internal class DataModule
// FILE: androidMain/org/koin/sample/androidx/multi/LibFooConsumer.kt
package org.koin.sample.androidx.multi

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.example.multi.FooBase

@Module
@ComponentScan
class LibFooConsumerModule

@Factory
class FooD(val b : FooB) : FooBase() {
    val text = "text D"
}
// FILE: androidMain/org/koin/sample/androidx/notcovered/IgnoredDefinition.kt
package org.koin.sample.androidx.notcovered

import org.koin.core.annotation.Single

@Single
class IgnoredDefinition
// FILE: androidMain/org/koin/sample/androidx/repository/Repository.kt
package org.koin.sample.androidx.repository

import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.koin.sample.androidx.data.TaskDatasource

@Single
class Repository(
    @Named("local")
    val local: TaskDatasource,
    @Named("remote")
    val remote: TaskDatasource
)
// FILE: androidMain/org/koin/sample/androidx/repository/RepositoryModule.kt
package org.koin.sample.androidx.repository

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan
class RepositoryModule

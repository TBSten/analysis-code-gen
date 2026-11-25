// PATTERN: KOI-O-0006
// TITLE: KMP の ViewModel 定義（koin-core-viewmodel を使う生成）
// PROCESSORS: koin
// EXPECT: ok
// OPTIONS: KOIN_USE_COMPOSE_VIEWMODEL=true
// ORIGIN: own
// STATUS: candidate
// FILE: shared/commonMain/com/example/vm/HomeViewModel.kt
package com.example.vm

import androidx.lifecycle.ViewModel
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Single
class HomeRepository

@KoinViewModel
class HomeViewModel(private val repository: HomeRepository) : ViewModel()

@KoinViewModel
class DetailViewModel(private val repository: HomeRepository, val id: String = "") : ViewModel()

@Module
@ComponentScan("com.example.vm")
class ViewModelModule
// FILE: shared/iosMain/com/example/vm/Platform.kt
package com.example.vm

import org.koin.core.annotation.Single

@Single
class IosPlatform

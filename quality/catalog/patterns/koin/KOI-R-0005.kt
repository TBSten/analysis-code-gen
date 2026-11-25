// PATTERN: KOI-R-0005
// TITLE: kotzilla-io/nowinandroid_koin_annotations / core:common / DispatchersKoinModule
// PROCESSORS: koin
// EXPECT: ok
// OPTIONS: KOIN_CONFIG_CHECK=true
// ORIGIN: real:kotzilla-io/nowinandroid_koin_annotations@6f0e1d2d8880/core/common/src/main/kotlin/com/google/samples/apps/nowinandroid/core/network/di/DispatchersKoinModule.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: common/main/com/google/samples/apps/nowinandroid/core/network/di/DispatchersKoinModule.kt
/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.core.network.di

import com.google.samples.apps.nowinandroid.core.network.Dispatcher
import com.google.samples.apps.nowinandroid.core.network.NiaDispatchers
import com.google.samples.apps.nowinandroid.core.network.NiaDispatchers.IO
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import javax.inject.Singleton

/**
 * For now using @Named qualifier manual bridge
 */
@Module
@Configuration
object DispatchersKoinModule {

    @Singleton // formally Provides in Hilt version
    @Dispatcher(IO)
    fun providesIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Singleton // formally Provides in Hilt version
    @Dispatcher(NiaDispatchers.Default)
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

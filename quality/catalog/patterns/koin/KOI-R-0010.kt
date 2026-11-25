// PATTERN: KOI-R-0010
// TITLE: kotzilla-io/nowinandroid_koin_annotations / app / JankStatsKoinModule
// PROCESSORS: koin
// EXPECT: ok
// OPTIONS: KOIN_CONFIG_CHECK=true
// ORIGIN: real:kotzilla-io/nowinandroid_koin_annotations@6f0e1d2d8880/app/src/main/kotlin/com/google/samples/apps/nowinandroid/di/JankStatsKoinModule.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: app/main/com/google/samples/apps/nowinandroid/di/JankStatsKoinModule.kt
/*
 * Copyright 2025 The Android Open Source Project
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

package com.google.samples.apps.nowinandroid.di

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.JankStats.OnFrameListener
import io.kotzilla.sdk.KotzillaSDK
import org.koin.android.annotation.ActivityScope
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

@Module
@Configuration
class JankStatsKoinModule {

    @ActivityScope
    fun jankStats(activity : ComponentActivity) : JankStats = JankStats.createAndTrack(activity.window, providesOnFrameListener())
}

fun providesOnFrameListener(): OnFrameListener = OnFrameListener { frameData ->
    // Make sure to only log janky frames.
    if (frameData.isJank) {
        // We're currently logging this but would better report it to a backend.
        Log.v("NiA Jank", frameData.toString())
        KotzillaSDK.log("NiA Jank - $frameData")
    }
}

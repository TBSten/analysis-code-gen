// PATTERN: KOI-R-0012
// TITLE: kotzilla-io/nowinandroid_koin_annotations / core:analytics / AnalyticsKoinModule
// PROCESSORS: koin
// EXPECT: ok
// OPTIONS: KOIN_CONFIG_CHECK=true
// ORIGIN: real:kotzilla-io/nowinandroid_koin_annotations@6f0e1d2d8880/core/analytics/src/demo/kotlin/com/google/samples/apps/nowinandroid/core/analytics/AnalyticsKoinModule.kt
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので
// FILE: analytics/demo/com/google/samples/apps/nowinandroid/core/analytics/AnalyticsKoinModule.kt
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

package com.google.samples.apps.nowinandroid.core.analytics

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

@Module
@Configuration
@ComponentScan
class AnalyticsKoinModule

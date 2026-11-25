// PATTERN: KOI-R-0031
// TITLE: msimonides/homerplayer2 / base / DispatcherProvider
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: real:msimonides/homerplayer2@ec5f4317b5fb/base/src/main/java/com/studio4plus/homerplayer2/base/DispatcherProvider.kt
// LICENSE: MIT
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: base/main/com/studio4plus/homerplayer2/base/DispatcherProvider.kt
/*
 * MIT License
 *
 * Copyright (c) 2024 Marcin Simonides
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.studio4plus.homerplayer2.base

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.annotation.Single

interface DispatcherProvider {
    val Main: CoroutineDispatcher
    val Io: CoroutineDispatcher
    val Computation: CoroutineDispatcher

}

@Single
class DefaultDispatcherProvider : DispatcherProvider {
    override val Main: CoroutineDispatcher = Dispatchers.Main
    override val Io: CoroutineDispatcher = Dispatchers.IO
    override val Computation: CoroutineDispatcher = Dispatchers.Default
}

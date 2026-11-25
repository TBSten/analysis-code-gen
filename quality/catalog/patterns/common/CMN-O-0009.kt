// PATTERN: CMN-O-0009
// TITLE: 背景コード: Android の androidMain と Android の API を使うクラス
// PROCESSORS: *
// EXPECT: ok
// ORIGIN: own
// STATUS: candidate
// FILE: shared/androidMain/com/example/bg/android/AndroidPlatform.kt
package com.example.bg.android

import android.content.Context
import android.os.Build

class AndroidPlatform(private val context: Context) {
    val sdk: Int = Build.VERSION.SDK_INT
    val packageName: String get() = context.packageName
}

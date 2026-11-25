// PATTERN: CRM-O-0002
// TITLE: KMP の中間ソースセットとプラットフォームに置いた状態遷移
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: own
// STATUS: candidate
// FILE: shared/commonMain/com/example/cream/kmp/ScreenState.kt
package com.example.cream.kmp

import me.tbsten.cream.CopyToChildren

@CopyToChildren
sealed interface ScreenState {
    val title: String

    data class Idle(override val title: String) : ScreenState
    data class Busy(override val title: String, val progress: Float = 0f) : ScreenState
}
// FILE: shared/appleMain/com/example/cream/kmp/AppleState.kt
package com.example.cream.kmp

import me.tbsten.cream.CopyTo

@CopyTo(ScreenState.Busy::class)
data class AppleIdle(val title: String)
// FILE: shared/iosMain/com/example/cream/kmp/IosOnly.kt
package com.example.cream.kmp

import me.tbsten.cream.CopyFrom

@CopyFrom(ScreenState.Idle::class)
data class IosSnapshot(val title: String, val takenAt: Long = 0L)

// PATTERN: CMN-O-0001
// TITLE: 背景コード: expect / actual を共通・中間・プラットフォームに置く
// PROCESSORS: *
// EXPECT: ok
// ORIGIN: own
// STATUS: candidate
// NOTE: PROCESSORS: * はどの Processor の入力にも混ぜる背景コード。生成結果を変えてはいけない
// FILE: shared/commonMain/com/example/bg/kmp/Platform.kt
package com.example.bg.kmp

expect class PlatformInfo() {
    val name: String
}

expect fun currentTimeMillis(): Long

expect object Dispatchers {
    val io: String
}
// FILE: shared/nativeMain/com/example/bg/kmp/NativeTime.kt
package com.example.bg.kmp

actual fun currentTimeMillis(): Long = 0L
// FILE: shared/iosMain/com/example/bg/kmp/Platform.ios.kt
package com.example.bg.kmp

actual class PlatformInfo actual constructor() {
    actual val name: String = "ios"
}

actual object Dispatchers {
    actual val io: String = "io"
}
// FILE: shared/jvmMain/com/example/bg/kmp/Platform.jvm.kt
package com.example.bg.kmp

actual class PlatformInfo actual constructor() {
    actual val name: String = "jvm"
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual object Dispatchers {
    actual val io: String = "io"
}

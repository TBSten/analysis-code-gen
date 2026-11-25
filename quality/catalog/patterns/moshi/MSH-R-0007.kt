// PATTERN: MSH-R-0007
// TITLE: simpledotorg/simple-android / app / SimpleVideo
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: real:simpledotorg/simple-android@482bb8c9bcdd/app/src/main/java/org/simple/clinic/simplevideo/SimpleVideo.kt
// LICENSE: MIT
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: app/main/org/simple/clinic/simplevideo/SimpleVideo.kt
package org.simple.clinic.simplevideo

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SimpleVideo(
    val url: String,
    val duration: String
)

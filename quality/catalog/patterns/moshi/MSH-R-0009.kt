// PATTERN: MSH-R-0009
// TITLE: simpledotorg/simple-android / app / StatePayload
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: real:simpledotorg/simple-android@482bb8c9bcdd/app/src/main/java/org/simple/clinic/appconfig/StatePayload.kt
// LICENSE: MIT
// STATUS: verified
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので
// FILE: app/main/org/simple/clinic/appconfig/StatePayload.kt
package org.simple.clinic.appconfig

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StatePayload(

    @Json(name = "name")
    val displayName: String
)

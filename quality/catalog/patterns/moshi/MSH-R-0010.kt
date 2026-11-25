// PATTERN: MSH-R-0010
// TITLE: simpledotorg/simple-android / app / StatesPayload
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: real:simpledotorg/simple-android@482bb8c9bcdd/app/src/main/java/org/simple/clinic/appconfig/StatesPayload.kt
// LICENSE: MIT
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: app/main/org/simple/clinic/appconfig/StatesPayload.kt
package org.simple.clinic.appconfig

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StatesPayload(

    @Json(name = "states")
    val states: List<StatePayload>
)

// PATTERN: MSH-O-0005
// TITLE: instantiateAnnotations=true を指定し、JsonQualifier を使わないクラスを生成する
// PROCESSORS: moshi
// EXPECT: ok
// OPTIONS: moshi.instantiateAnnotations=true
// ORIGIN: own
// STATUS: verified
// NOTE: JsonQualifier を使う形（MSH-O-0001）は Moshi 1.15.2 が KSP2 で落ちるので rejected。オプションを渡しても生成が壊れないことだけをここで確かめる（ADR-072）
// FILE: main/com/example/moshi/instantiate/Profile.kt
package com.example.moshi.instantiate

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Profile(
    val id: Long,
    @Json(name = "display_name") val displayName: String,
    val bio: String? = null,
)

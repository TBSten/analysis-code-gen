// PATTERN: MSH-R-0004
// TITLE: simpledotorg/simple-android / app / RetentionType
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: real:simpledotorg/simple-android@482bb8c9bcdd/app/src/main/java/org/simple/clinic/patient/onlinelookup/api/RetentionType.kt
// LICENSE: MIT
// STATUS: verified
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので
// FILE: app/main/org/simple/clinic/patient/onlinelookup/api/RetentionType.kt
package org.simple.clinic.patient.onlinelookup.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class RetentionType {

  @Json(name = "temporary")
  Temporary,

  @Json(name = "permanent")
  Permanent,

  // Fallback value for newly added enum types for older app versions
  Unknown
}

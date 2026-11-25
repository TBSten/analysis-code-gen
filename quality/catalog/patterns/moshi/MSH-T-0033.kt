// PATTERN: MSH-T-0033
// TITLE: 実行時テスト / CompileOnlyTests / KeysWithSpaces
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/codegen-only/src/test/kotlin/com/squareup/moshi/kotlin/codegen/CompileOnlyTests.kt#KeysWithSpaces
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: KeysWithSpaces.kt
package com.squareup.moshi.kotlin.codegen

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KeysWithSpaces(
  @Json(name = "1. Information") val information: String,
  @Json(name = "2. Symbol") val symbol: String,
  @Json(name = "3. Last Refreshed") val lastRefreshed: String,
  @Json(name = "4. Interval") val interval: String,
  @Json(name = "5. Output Size") val size: String,
  @Json(name = "6. Time Zone") val timeZone: String
)

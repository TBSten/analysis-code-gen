// PATTERN: MSH-O-0002
// TITLE: 生成クラスに付ける @Generated を指定する・ProGuard ルールを出さない
// PROCESSORS: moshi
// EXPECT: ok
// OPTIONS: moshi.generated=javax.annotation.processing.Generated, moshi.generateProguardRules=false
// ORIGIN: own
// STATUS: verified
// FILE: main/com/example/moshi/generated/Profile.kt
package com.example.moshi.generated

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Profile(
    val id: String,
    @Json(name = "display_name") val displayName: String?,
    @Json(ignore = true) val cachedAt: Long = 0L,
    @Transient val localOnly: Boolean = false,
)

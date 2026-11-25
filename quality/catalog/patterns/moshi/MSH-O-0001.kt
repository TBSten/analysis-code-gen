// PATTERN: MSH-O-0001
// TITLE: JsonQualifier を反射で引く従来の生成に戻す（instantiateAnnotations=false）
// PROCESSORS: moshi
// EXPECT: ok
// OPTIONS: moshi.instantiateAnnotations=false
// ORIGIN: own
// STATUS: candidate
// FILE: main/com/example/moshi/qualifier/Event.kt
package com.example.moshi.qualifier

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonQualifier

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class EpochMillis

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class Trimmed(val maxLength: Int = 0)

@JsonClass(generateAdapter = true)
data class Event(
    @EpochMillis val at: Long,
    @Trimmed(maxLength = 64) val title: String,
    val tags: List<@Trimmed String> = emptyList(),
)

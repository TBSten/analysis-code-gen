// PATTERN: MSH-O-0001
// TITLE: JsonQualifier を反射で引く従来の生成に戻す（instantiateAnnotations=false）
// PROCESSORS: moshi
// EXPECT: ok
// OPTIONS: moshi.instantiateAnnotations=false
// ORIGIN: own
// STATUS: rejected
// NOTE: K（KSP2 2.3.14）: K でビルドが失敗した: Error preparing Event: jdk.proxy1/jdk.proxy1.$Proxy42.value(Unknown Source) com.squareup.moshi.kotlin.codegen.ksp.MoshiApiUtilKt.generator(MoshiApiUtil.kt:77) com.squareup.moshi.kotlin.codegen.ksp.JsonClassSymbolProcessor.adapterGenerator(JsonClassSymbolProcessorProvider.kt:122) com.squareup.moshi.kotlin.codegen.ksp.JsonClassSymbolProcessor.process(JsonClassSymbolProcessorProvider.kt:
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
    @Trimmed val tags: List<String> = emptyList(),
)

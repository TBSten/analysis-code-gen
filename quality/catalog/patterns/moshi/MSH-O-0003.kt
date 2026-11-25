// PATTERN: MSH-O-0003
// TITLE: Gradle モジュールをまたぐ DTO（core の型を app の DTO が持つ）と Java の enum
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: own
// STATUS: candidate
// FILE: core/main/com/example/moshi/core/Money.kt
package com.example.moshi.core

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Money(val amount: Long, val currency: Currency)
// FILE: core/main/com/example/moshi/core/Currency.java
package com.example.moshi.core;

public enum Currency {
    JPY,
    USD,
}
// FILE: app/main/com/example/moshi/app/Order.kt
package com.example.moshi.app

import com.example.moshi.core.Money
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Order(
    val id: String,
    val total: Money,
    @Json(ignore = true) val note: String? = null,
    val lines: Map<String, List<Money>> = emptyMap(),
)

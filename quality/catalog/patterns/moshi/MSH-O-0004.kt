// PATTERN: MSH-O-0004
// TITLE: sealed の各サブクラスに個別のアダプタ・value class を含むプロパティ・companion
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: own
// STATUS: verified
// FILE: main/com/example/moshi/sealed/Shape.kt
package com.example.moshi.sealed

import com.squareup.moshi.JsonClass

@JvmInline
value class ShapeId(val raw: String)

sealed class Shape {
    abstract val id: String

    @JsonClass(generateAdapter = true)
    data class Circle(override val id: String, val radius: Double) : Shape()

    @JsonClass(generateAdapter = true)
    data class Rect(override val id: String, val w: Double, val h: Double = w) : Shape() {
        companion object {
            const val TYPE = "rect"
        }
    }
}

@JsonClass(generateAdapter = true)
class Canvas<out T : Shape>(val shapes: List<T>, val selected: Set<*>? = null)

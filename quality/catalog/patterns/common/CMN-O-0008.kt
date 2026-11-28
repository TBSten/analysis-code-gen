// PATTERN: CMN-O-0008
// TITLE: 背景コード: use-site target・配列引数・クラスリテラル引数・入れ子のアノテーション・繰り返し可能なアノテーション
// PROCESSORS: *
// EXPECT: ok
// ORIGIN: own
// STATUS: verified
// FILE: main/com/example/bg/annotations/Annotations.kt
package com.example.bg.annotations

import kotlin.reflect.KClass

@Repeatable
@Retention(AnnotationRetention.BINARY)
annotation class Tag(val value: String)

annotation class Meta(val tags: Array<Tag> = [], val types: Array<KClass<*>> = [], val level: Level = Level.LOW)

enum class Level { LOW, HIGH }

@Tag("a")
@Tag("b")
@Meta(tags = [Tag("x")], types = [String::class, Int::class], level = Level.HIGH)
class Annotated(
    @param:Tag("p") @property:Tag("q") val name: String,
    @field:Volatile var counter: Int = 0,
) {
    @get:JvmName("isReady")
    val ready: Boolean get() = counter > 0

    @set:Tag("s")
    var label: String = ""

    lateinit var late: String

    val lazyValue by lazy { name.length }
}

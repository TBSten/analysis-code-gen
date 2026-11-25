// PATTERN: CMN-O-0004
// TITLE: 背景コード: 再帰境界・where 句・変位・スター投影・reified・入れ子の型引数
// PROCESSORS: *
// EXPECT: ok
// ORIGIN: own
// STATUS: candidate
// FILE: main/com/example/bg/generics/Generics.kt
package com.example.bg.generics

interface Node<T : Node<T>> {
    val children: List<T>
}

class Tree(override val children: List<Tree>) : Node<Tree>

class Registry<in K, out V>(private val lookup: (K) -> V) where K : Any, K : Comparable<K> {
    fun get(key: K): V = lookup(key)
}

fun describe(items: Map<String, List<Set<*>>>): Int = items.size

inline fun <reified T> typeName(): String = T::class.simpleName ?: "?"

class Pair2<A, B>(val first: A, val second: B?) {
    inner class View(val label: String)

    class Builder<A> {
        class Deep<Z>(val z: Z)
    }
}

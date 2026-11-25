// PATTERN: CRM-O-0003
// TITLE: 関数名の接頭辞と命名方針・ドットの扱いを変える
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=into, cream.copyFunNamingStrategy=simple-name, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=full-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL, cream.autoValueClassMapping=true
// TAGS: cream.family=options
// ORIGIN: own
// STATUS: candidate
// FILE: main/com/example/cream/options/Cart.kt
package com.example.cream.options

import me.tbsten.cream.CopyToChildren

@JvmInline
value class Quantity(val value: Int)

@CopyToChildren
sealed interface Cart {
    val items: List<String>

    data class Empty(override val items: List<String> = emptyList()) : Cart
    data class Filled(override val items: List<String>, val quantity: Quantity) : Cart
    data object Closed : Cart {
        override val items: List<String> = emptyList()
    }
}

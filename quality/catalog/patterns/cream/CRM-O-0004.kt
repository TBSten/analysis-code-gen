// PATTERN: CRM-O-0004
// TITLE: 実アプリ風の MVI: 画面状態・イベント・ドメインの 3 ファイルで cream を使う
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: own
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、典型的な使い方を自作で補う（ADR-020）
// FILE: main/com/example/cream/mvi/Order.kt
package com.example.cream.mvi

data class Order(val id: String, val items: List<Item>, val total: Long)

data class Item(val sku: String, val count: Int)
// FILE: main/com/example/cream/mvi/OrderUiState.kt
package com.example.cream.mvi

import me.tbsten.cream.CopyTo
import me.tbsten.cream.CopyToChildren
import me.tbsten.cream.ParentOptional

@CopyToChildren
sealed interface OrderUiState {
    val orderId: String

    data class Loading(override val orderId: String) : OrderUiState

    @CopyTo(Loading::class)
    data class Loaded(
        override val orderId: String,
        @ParentOptional val order: Order,
        val editing: Boolean = false,
    ) : OrderUiState

    data class Failed(override val orderId: String, @ParentOptional val message: String) : OrderUiState
}
// FILE: main/com/example/cream/mvi/Reducer.kt
package com.example.cream.mvi

fun reduce(state: OrderUiState, loaded: Order?): OrderUiState =
    when {
        loaded != null -> state.copyToOrderUiStateLoaded(order = loaded)
        else -> state.copyToOrderUiStateFailed(message = "not found")
    }

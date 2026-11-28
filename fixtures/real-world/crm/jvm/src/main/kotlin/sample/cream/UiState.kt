package sample.cream

import me.tbsten.cream.CopyToChildren

/** sealed の親から、入れ子の sealed を含むすべての具象の子へ写す関数を作る。 */
@CopyToChildren
sealed interface UiState {
    val userName: String

    data class Loading(
        override val userName: String,
    ) : UiState

    sealed interface Success : UiState {
        val count: Int

        data class Done(
            override val userName: String,
            override val count: Int,
        ) : Success

        data class Refreshing(
            override val userName: String,
            override val count: Int,
            val progress: Double,
        ) : Success
    }
}

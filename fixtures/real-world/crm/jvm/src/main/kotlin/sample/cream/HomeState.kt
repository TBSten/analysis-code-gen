package sample.cream

import me.tbsten.cream.CopyTo

/** sealed interface の状態遷移。Loading（object）から Success・Error へ写す。 */
sealed interface HomeState {
    val userName: String

    @CopyTo(Success::class, Error::class)
    data class Loading(
        override val userName: String,
    ) : HomeState

    data class Success(
        override val userName: String,
        val data: List<String>,
    ) : HomeState

    data class Error(
        override val userName: String,
        val message: String,
        val retryable: Boolean = true,
    ) : HomeState
}

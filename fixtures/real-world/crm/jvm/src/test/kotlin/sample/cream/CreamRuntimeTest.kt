package sample.cream

import kotlin.test.Test
import kotlin.test.assertEquals

// 生成された写し関数を呼ぶ実行時テスト。K（KSP2）・B（Bridge）・N（Native 書き直し版）で同じテストが通る（ADR-037）
class CreamRuntimeTest {
    private val response = ItemResponse(itemId = "i-1", name = "Apple", price = 120, etag = "v1")

    @Test
    fun copyToCopiesPropertiesWithTheSameName() {
        assertEquals(Item(itemId = "i-1", name = "Apple", price = 120), response.copyToItem())
    }

    @Test
    fun copyToCanOverrideProperties() {
        assertEquals(Item(itemId = "i-1", name = "Apple", price = 98), response.copyToItem(price = 98))
    }

    @Test
    fun copyToSealedSiblingKeepsSharedProperties() {
        val loading = HomeState.Loading(userName = "tbsten")
        val success: HomeState.Success = loading.copyToHomeStateSuccess(data = listOf("a", "b"))
        assertEquals(HomeState.Success(userName = "tbsten", data = listOf("a", "b")), success)
    }

    @Test
    fun copyToRequiresUnmatchedParameters() {
        // 写し元に無いパラメータ（message・retryable）は、写し先に既定値があっても呼び出し側が渡す
        val error = HomeState.Loading(userName = "tbsten").copyToHomeStateError(message = "boom", retryable = false)
        assertEquals(HomeState.Error(userName = "tbsten", message = "boom", retryable = false), error)
    }

    @Test
    fun copyFromGeneratesFunctionOnTheSource() {
        val form = UserForm(name = "Alice", age = 20, draft = true)
        assertEquals(User(name = "Alice", age = 20, email = null), form.copyToUser(email = null))
        assertEquals(User(name = "Alice", age = 21, email = "a@example.com"), form.copyToUser(age = 21, email = "a@example.com"))
    }

    @Test
    fun copyToChildrenReachesNestedLeaves() {
        val state: UiState = UiState.Loading(userName = "tbsten")
        assertEquals(UiState.Success.Done(userName = "tbsten", count = 3), state.copyToUiStateSuccessDone(count = 3))
        val refreshing = state.copyToUiStateSuccessRefreshing(count = 3, progress = 0.5)
        assertEquals(UiState.Success.Refreshing(userName = "tbsten", count = 3, progress = 0.5), refreshing)
        assertEquals(UiState.Loading(userName = "other"), state.copyToUiStateLoading(userName = "other"))
    }
}

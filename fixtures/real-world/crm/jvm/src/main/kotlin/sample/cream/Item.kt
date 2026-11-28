package sample.cream

/** ドメイン層のモデル。[ItemResponse] から写す先。 */
data class Item(
    val itemId: String,
    val name: String,
    val price: Int,
)

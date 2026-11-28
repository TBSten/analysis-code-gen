package sample.cream

import me.tbsten.cream.CopyTo

/** データ層のモデル。同じ名前のプロパティが [Item] へ写る（etag は Item に無いので写らない）。 */
@CopyTo(Item::class)
data class ItemResponse(
    val itemId: String,
    val name: String,
    val price: Int,
    val etag: String,
)

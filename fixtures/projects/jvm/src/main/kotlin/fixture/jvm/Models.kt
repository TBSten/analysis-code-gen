package fixture.jvm

import me.tbsten.analysis.code.gen.tp.Mapped
import me.tbsten.analysis.code.gen.tp.Registered

@Mapped
@Registered
data class User(
    val id: Int,
    val name: String,
    val tags: List<String> = emptyList(),
)

@Mapped
@Registered
class Order(
    val id: Long,
    val owner: User?,
    note: String,
) {
    val summary: String = "$id:$note"

    @Mapped
    data class Line(val sku: String, val quantity: Int)
}

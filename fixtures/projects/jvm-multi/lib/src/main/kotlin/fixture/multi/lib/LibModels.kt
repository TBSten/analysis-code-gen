package fixture.multi.lib

import me.tbsten.analysiscodegen.tp.Mapped
import me.tbsten.analysiscodegen.tp.Registered

@Mapped
@Registered
data class LibUser(val id: Int, val name: String)

/** :app の Processor が型として解析する公開宣言（GRD-09）。 */
interface LibService {
    fun greet(user: LibUser): String
}

package fixture.android.app

import me.tbsten.analysis.code.gen.tp.Mapped
import me.tbsten.analysis.code.gen.tp.Registered

/** src/paid だけにあるクラス。paid の Variant でだけ解析される（GRD-11）。 */
@Mapped
@Registered
data class PaidFeature(val seats: Int)

object Flavor {
    const val NAME: String = "paid"

    fun describe(): String = PaidFeatureMapper.toMap(PaidFeature(3)).toString()
}

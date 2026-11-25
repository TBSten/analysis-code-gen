package fixture.android.app

import me.tbsten.analysiscodegen.tp.Mapped
import me.tbsten.analysiscodegen.tp.Registered

/** src/free だけにあるクラス。free の Variant でだけ解析される（GRD-11）。 */
@Mapped
@Registered
data class FreeFeature(val limit: Int)

object Flavor {
    const val NAME: String = "free"

    fun describe(): String = FreeFeatureMapper.toMap(FreeFeature(3)).toString()
}

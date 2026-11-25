package fixture.android.app

import me.tbsten.analysis.code.gen.tp.generated.TpRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class FreeVariantTest {
    @Test
    fun variantごとの集約() {
        assertEquals(listOf("fixture.android.app.DeviceInfo", "fixture.android.app.FreeFeature"), TpRegistry.entries)
        assertEquals("{limit=3}", Flavor.describe())
    }
}

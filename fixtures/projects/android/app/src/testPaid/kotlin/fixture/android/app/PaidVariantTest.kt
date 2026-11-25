package fixture.android.app

import me.tbsten.analysis.code.gen.tp.generated.TpRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class PaidVariantTest {
    @Test
    fun variantごとの集約() {
        assertEquals(listOf("fixture.android.app.DeviceInfo", "fixture.android.app.PaidFeature"), TpRegistry.entries)
        assertEquals("{seats=3}", Flavor.describe())
    }
}

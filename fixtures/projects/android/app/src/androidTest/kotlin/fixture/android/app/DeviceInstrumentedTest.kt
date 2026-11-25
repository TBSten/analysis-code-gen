package fixture.android.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** androidTest から main の生成物が見える（GRD-13）。CI ではコンパイルだけを確かめる（エミュレータは使わない）。 */
@RunWith(AndroidJUnit4::class)
class DeviceInstrumentedTest {
    @Test
    fun mainの生成物が見える() {
        assertEquals("android.content.Context?", DeviceInfoMapper.propertyTypes["context"])
    }
}

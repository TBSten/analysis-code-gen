package fixture.android.app

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceTest {
    @Test
    fun androidの型を解決できる() {
        assertEquals("android.content.Context?", DeviceInfoMapper.propertyTypes["context"])
        assertEquals("fixture.android.lib.LibModel", DeviceInfoMapper.propertyTypes["lib"])
        assertEquals("android.net.Uri?", fixture.android.lib.LibModelMapper.propertyTypes["uri"])
    }
}

package fixture.custom

import kotlin.test.Test
import kotlin.test.assertEquals

class CustomTest {
    @Test
    fun 中間SourceSetの生成物をjvmで使える() {
        assertEquals("jvm:{id=1, name=a}", describeJvmJs())
        assertEquals("kotlin.Int", JvmJsModelMapper.propertyTypes["id"])
    }
}

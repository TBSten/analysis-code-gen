package fixture.multi.app

import fixture.multi.app.generated.AppRegistry
import fixture.multi.lib.LibUser
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTest {
    @Test
    fun libの宣言と生成物をappで使える() {
        assertEquals("fixture.multi.lib.LibUser", AppUserMapper.propertyTypes["lib"])
        assertEquals("fixture.multi.lib.LibUserMapper", NeedsLibMapperResolved.targetName)
        assertEquals("hello a", AppService().greet(LibUser(1, "a")))
    }

    @Test
    fun 集約はモジュールごと() {
        assertEquals(listOf("fixture.multi.app.AppService", "fixture.multi.app.AppUser"), AppRegistry.entries)
        assertEquals("fixture.multi.lib.LibUser", describe()[2])
    }
}

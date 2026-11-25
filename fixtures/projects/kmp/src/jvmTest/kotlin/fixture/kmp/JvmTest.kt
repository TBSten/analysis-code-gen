package fixture.kmp

import me.tbsten.analysis.code.gen.tp.generated.TpRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmTest {
    @Test
    fun jvmの集約にはjvmのクラスだけが入る() {
        assertEquals("jvm", platformName())
        assertEquals(listOf("fixture.kmp.JvmOnly"), TpRegistry.entries)
        assertEquals("jvm", TpRegistry.platform)
    }
}

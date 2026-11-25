package fixture.kmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedTest {
    @Test
    fun commonの生成物を全ターゲットで使える() {
        assertEquals(mapOf("id" to 1, "label" to "a", "tags" to emptyList<String>()), SharedMapper.toMap(Shared(1, "a", emptyList())))
        assertEquals("kotlin.collections.List<kotlin.String>", SharedMapper.propertyTypes["tags"])
        assertEquals(2, StepChain2(StepChain1(Step())).step)
        assertEquals("fixture.kmp.StepChain2", AfterStepsResolved.targetName)
    }

    @Test
    fun actualはターゲットごと() {
        assertTrue(platformName() in setOf("jvm", "js", "wasm", "native"), platformName())
    }
}

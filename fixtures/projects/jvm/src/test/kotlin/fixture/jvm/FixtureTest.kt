package fixture.jvm

import me.tbsten.analysis.code.gen.tp.Mapped
import me.tbsten.analysis.code.gen.tp.generated.TpRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

/** test SourceSet だけにあるクラス。test 向けの実行で TestOnlyMapper が作られる（GRD-16）。 */
@Mapped
data class TestOnly(val value: Int)

class FixtureTest {
    @Test
    fun mainの生成物がtestから見える() {
        assertEquals(mapOf("id" to 1, "name" to "alice", "tags" to emptyList<String>()), UserMapper.toMap(User(1, "alice")))
        assertEquals(listOf("fixture.jvm.Order", "fixture.jvm.User"), TpRegistry.entries)
        assertEquals("kotlin.collections.List<kotlin.String>", UserMapper.propertyTypes["tags"])
        assertEquals("fixture.jvm.User?", OrderMapper.propertyTypes["owner"])
    }

    @Test
    fun testの生成物() {
        assertEquals(mapOf("value" to 3), TestOnlyMapper.toMap(TestOnly(3)))
    }

    @Test
    fun ラウンドをまたぐ生成物() {
        assertEquals("3", describe()[3])
        assertEquals("fixture.jvm.PipelineChain3", WaitsForChainResolved.targetName)
        assertEquals("fixture.jvm.Report", ReportJavaRef.name)
    }
}

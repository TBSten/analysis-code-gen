package com.example.hello

import me.tbsten.analysiscodegen.testing.TestEngine
import me.tbsten.analysiscodegen.testing.runCodegenTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BridgeCompatibilityTest {
    @Test
    fun `KSP2 と Bridge で同じものが生成される`() {
        val (ksp2, bridge) = listOf(TestEngine.Ksp2, TestEngine.AnalysisCodegen).map { engine ->
            runCodegenTest(engine) {
                kotlin("com/example/Greeter.kt", "package com.example\n\nannotation class Hello\n\n@Hello class Greeter")
                ksp(HelloSymbolProcessorProvider())
            }
        }
        assertTrue(bridge.isSuccess, bridge.messages)
        assertEquals(ksp2.generatedFiles, bridge.generatedFiles)
    }
}

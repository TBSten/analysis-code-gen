package com.example.hello

import me.tbsten.analysiscodegen.testing.TestEngine
import me.tbsten.analysiscodegen.testing.runCodegenScenario
import kotlin.test.Test
import kotlin.test.assertEquals

class HelloScenarioTest {
    @Test
    fun `ファイルを消すと生成物も消える`() {
        val results = runCodegenScenario(TestEngine.AnalysisCodegen) {
            initial {
                kotlin("com/example/Hello.kt", "package com.example\n\nannotation class Hello")
                kotlin("com/example/A.kt", "package com.example\n\n@Hello class A")
                kotlin("com/example/B.kt", "package com.example\n\n@Hello class B")
                native(HelloProcessorProvider())
            }
            step("B を消す") { delete("com/example/B.kt") }
        }
        val generated = results.last().result.generatedFiles.keys
        assertEquals(setOf("kotlin/com/example/AHello.kt"), generated)
    }
}

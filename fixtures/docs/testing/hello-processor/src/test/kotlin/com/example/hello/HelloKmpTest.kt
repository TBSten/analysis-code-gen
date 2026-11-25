package com.example.hello

import me.tbsten.analysis.code.gen.testing.TestEngine
import me.tbsten.analysis.code.gen.testing.runCodegenTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HelloKmpTest {
    @Test
    fun `commonMain のクラスには commonMain で生成する`() {
        val result = runCodegenTest(TestEngine.AnalysisCodegen) {
            sourceSet("commonMain") {
                kotlin("com/example/Greeter.kt", "package com.example\n\n@Hello class Greeter")
                kotlin("com/example/Hello.kt", "package com.example\n\nannotation class Hello")
            }
            sourceSet("jvmMain") { dependsOn += "commonMain" }
            native(HelloProcessorProvider())
        }
        assertEquals(listOf("commonMain/kotlin/com/example/GreeterHello.kt"), result.generatedFiles.keys.toList())
    }
}

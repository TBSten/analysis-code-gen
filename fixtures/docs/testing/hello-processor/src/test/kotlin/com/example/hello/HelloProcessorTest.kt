package com.example.hello

import me.tbsten.analysis.code.gen.testing.TestEngine
import me.tbsten.analysis.code.gen.testing.runCodegenTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HelloProcessorTest {
    private fun runHello() = runCodegenTest(TestEngine.AnalysisCodegen) {
        kotlin("com/example/Greeter.kt", """
            package com.example
            annotation class Hello
            @Hello class Greeter
        """)
        native(HelloProcessorProvider())
    }

    @Test
    fun `hello 関数のファイルが生成される`() {
        val result = runHello()
        assertTrue(result.isSuccess, result.messages)
        assertEquals(
            "package com.example\n\nfun Greeter.hello(): String = \"Hello, Greeter!\"\n",
            result.generatedFile("com/example/GreeterHello.kt"),
        )
    }

    @Test
    fun `生成された hello を呼べる`() {
        val result = runHello()
        val greeter = result.loadClass("com.example.Greeter").getDeclaredConstructor().newInstance()
        val hello = result.loadClass("com.example.GreeterHelloKt").getMethod("hello", greeter.javaClass)
        assertEquals("Hello, Greeter!", hello.invoke(null, greeter))
    }

    @Test
    fun `@Hello が無ければ何も生成しない`() {
        val result = runCodegenTest(TestEngine.AnalysisCodegen) {
            kotlin("com/example/Plain.kt", "package com.example\n\nclass Plain")
            native(HelloProcessorProvider())
        }
        assertTrue(result.isSuccess, result.messages)
        assertTrue(result.generatedFiles.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }
}

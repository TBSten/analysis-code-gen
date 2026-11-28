package sample.inject

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

// 生成された Component で依存を解決する実行時テスト。K（KSP2）と B（Bridge）で同じテストが通る（ADR-037）
class KotlinInjectRuntimeTest {
    private val component = AppComponent::class.create(42L)

    @Test
    fun resolvesConstructorInjectionAndProvides() {
        assertEquals("Hello, Alice at 42", component.useCase("Alice"))
    }

    @Test
    fun scopedInstanceIsShared() {
        assertSame(component.repository, component.useCase.repository)
        assertSame(component.repository, component.repository)
    }

    @Test
    fun unscopedInstanceIsCreatedEachTime() {
        assertNotSame(component.useCase, component.useCase)
    }
}

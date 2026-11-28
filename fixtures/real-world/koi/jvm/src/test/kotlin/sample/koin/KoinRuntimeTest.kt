package sample.koin

import org.koin.dsl.koinApplication
import org.koin.ksp.generated.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

// 生成された module 定義で依存を解決する実行時テスト。K（KSP2）と B（Bridge）で同じテストが通る（ADR-037）
class KoinRuntimeTest {
    private val koin = koinApplication { modules(AppModule().module) }.koin

    @Test
    fun resolvesFactoryWithSingleDependency() {
        assertEquals("Hello, Alice", koin.get<GreetingUseCase>()("Alice"))
    }

    @Test
    fun singleIsSharedAndFactoryIsNew() {
        assertSame(koin.get<GreetingRepository>(), koin.get<GreetingUseCase>().repository)
        assertNotSame(koin.get<GreetingUseCase>(), koin.get<GreetingUseCase>())
    }
}

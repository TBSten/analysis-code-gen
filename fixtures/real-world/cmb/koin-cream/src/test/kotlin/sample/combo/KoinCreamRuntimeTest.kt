package sample.combo

import org.koin.dsl.koinApplication
import org.koin.ksp.generated.module
import kotlin.test.Test
import kotlin.test.assertEquals

// Koin の生成物（module 定義）と cream.kt の生成物（写し関数）を同じテストで使う。K と B で同じテストが通る（ADR-037）
class KoinCreamRuntimeTest {
    private val koin = koinApplication { modules(AppModule().module) }.koin

    @Test
    fun koinInjectsRepositoryThatUsesCreamCopy() {
        assertEquals(Profile(id = "1", displayName = "user-1"), koin.get<ProfileRepository>().load("1"))
    }

    @Test
    fun creamCopyCanOverride() {
        val response = ProfileResponse(id = "2", displayName = "before", updatedAt = 1L)
        assertEquals(Profile(id = "2", displayName = "after"), response.copyToProfile(displayName = "after"))
    }
}

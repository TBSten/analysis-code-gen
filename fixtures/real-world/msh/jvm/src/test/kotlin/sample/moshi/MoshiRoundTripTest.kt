package sample.moshi

import com.squareup.moshi.Moshi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// 生成された JsonAdapter で JSON を往復させる実行時テスト。K（KSP2）と B（Bridge）で同じテストが通る（ADR-037）
class MoshiRoundTripTest {
    private val adapter = Moshi.Builder().build().adapter(Person::class.java)

    @Test
    fun usesGeneratedAdapter() {
        // リフレクションの KotlinJsonAdapter ではなく、codegen の生成物が使われている
        assertTrue(adapter.toString().contains("GeneratedJsonAdapter(Person)"), adapter.toString())
    }

    @Test
    fun roundTrip() {
        val person = Person(name = "Alice", age = 20, address = Address(city = "Tokyo", zip = null), tags = listOf("a", "b"))
        val json = adapter.toJson(person)
        assertEquals(person, adapter.fromJson(json))
        assertTrue(json.contains("\"full_name\":\"Alice\""), json)
    }

    @Test
    fun appliesDefaultValues() {
        assertEquals(Person(name = "Bob"), adapter.fromJson("""{"full_name":"Bob"}"""))
    }
}

package sample.moshi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** 名前の付け替え・既定値・入れ子・リストを含む。Moshi codegen が PersonJsonAdapter を作る。 */
@JsonClass(generateAdapter = true)
data class Person(
    @Json(name = "full_name") val name: String,
    val age: Int = 0,
    val address: Address? = null,
    val tags: List<String> = emptyList(),
)

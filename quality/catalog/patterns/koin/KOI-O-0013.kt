// PATTERN: KOI-O-0013
// TITLE: Koin + Moshi + cream.kt: API の DTO を変換してリポジトリから返す
// PROCESSORS: koin, moshi, cream
// EXPECT: ok
// ORIGIN: own
// STATUS: candidate
// FILE: main/com/example/triple/Dto.kt
package com.example.triple

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import me.tbsten.cream.CopyTo

@JsonClass(generateAdapter = true)
@CopyTo(Article::class)
data class ArticleDto(
    val id: Long,
    @Json(name = "title_text") val title: String,
    val tags: List<String> = emptyList(),
)

data class Article(val id: Long, val title: String, val tags: List<String>, val read: Boolean)
// FILE: main/com/example/triple/ArticleRepository.kt
package com.example.triple

import com.squareup.moshi.Moshi
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Single
class ArticleRepository(private val moshi: Moshi) {
    fun parse(json: String): Article? =
        moshi.adapter(ArticleDto::class.java).fromJson(json)?.copyToArticle(read = false)
}

@Module
@ComponentScan
class ArticleModule {
    @Single
    fun moshi(): Moshi = Moshi.Builder().build()
}

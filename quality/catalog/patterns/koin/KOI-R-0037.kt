// PATTERN: KOI-R-0037
// TITLE: darriousliu/PiPixiv / common:repository / NovelReadLaterSource
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: real:darriousliu/PiPixiv@1a2e76353cc3/common/repository/src/commonMain/kotlin/com/mrl/pixiv/common/repository/NovelReadLaterSource.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: repository/commonMain/com/mrl/pixiv/common/repository/NovelReadLaterSource.kt
package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.Novel
import org.koin.core.annotation.Single

data class NovelReadLaterSourceContent(
    val novel: Novel,
    val text: String,
)

interface NovelReadLaterSource {
    suspend fun load(novelId: Long): NovelReadLaterSourceContent
}

@Single(binds = [NovelReadLaterSource::class])
class PixivNovelReadLaterSource : NovelReadLaterSource {
    override suspend fun load(novelId: Long): NovelReadLaterSourceContent {
        val novel = PixivRepository.getNovelDetail(novelId).novel
        val html = PixivRepository.getNovelContent(novelId)
        val text = requireNotNull(NovelContentParser.extract(html)) {
            "Novel content is unavailable."
        }.text
        require(text.isNotBlank()) {
            "Novel content is empty."
        }
        return NovelReadLaterSourceContent(novel = novel, text = text)
    }
}

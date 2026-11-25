// PATTERN: KOI-R-0041
// TITLE: darriousliu/PiPixiv / common:repository / HistoryIllustPagingSource
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: real:darriousliu/PiPixiv@1a2e76353cc3/common/repository/src/commonMain/kotlin/com/mrl/pixiv/common/repository/paging/HistoryIllustPagingSource.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: repository/commonMain/com/mrl/pixiv/common/repository/paging/HistoryIllustPagingSource.kt
package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.util.filterBlockedTags
import com.mrl.pixiv.common.repository.util.filterNormalIllust
import com.mrl.pixiv.common.repository.util.queryParams
import org.koin.core.annotation.Factory

@Factory
class HistoryIllustPagingSource : PagingSource<String, Illust>() {
    override fun getRefreshKey(state: PagingState<String, Illust>): String? {
        return null
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Illust> {
        return try {
            val resp = if (params.key.isNullOrEmpty()) {
                PixivRepository.getUserBrowsingHistoryIllusts()
            } else {
                PixivRepository.loadMoreUserBrowsingHistoryIllusts(
                    params.key?.queryParams ?: emptyMap()
                )
            }
            val query = resp.nextUrl
            val illusts = if (requireUserPreferenceValue.isR18Enabled) {
                resp.illusts.distinctBy { it.id }
            } else {
                resp.illusts.distinctBy { it.id }.filterNormalIllust()
            }.filterBlockedTags()
            LoadResult.Page(
                data = illusts,
                prevKey = params.key,
                nextKey = query
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}

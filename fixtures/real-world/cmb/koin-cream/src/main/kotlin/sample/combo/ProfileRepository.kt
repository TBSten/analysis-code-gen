package sample.combo

import org.koin.core.annotation.Single

/** Koin が注入し、中で cream.kt の生成関数を使う。 */
@Single
class ProfileRepository {
    fun load(id: String): Profile = ProfileResponse(id = id, displayName = "user-$id", updatedAt = 0L).copyToProfile()
}

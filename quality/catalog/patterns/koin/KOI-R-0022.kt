// PATTERN: KOI-R-0022
// TITLE: codymikol/git-down / root / StashService
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: real:codymikol/git-down@dfbb1e11eab9/src/main/kotlin/com/codymikol/services/StashService.kt
// LICENSE: MIT
// STATUS: rejected
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので K（KSP2 2.3.14）: K でビルドが失敗した: Unresolved reference 'data'.
// FILE: root/main/com/codymikol/services/StashService.kt
package com.codymikol.services

import com.codymikol.data.stash.StashListItem
import com.codymikol.extensions.applyStash
import com.codymikol.extensions.dropStash
import com.codymikol.extensions.saveStash
import com.codymikol.state.GitDownState
import org.koin.core.annotation.Single

@Single
class StashService {

    suspend fun saveStash(message: String, includeUntrackedFiles: Boolean) {
        GitDownState.git.value.saveStash(message, includeUntrackedFiles)
    }
    
    suspend fun applyStash(stash: StashListItem) {
        GitDownState.git.value.applyStash(stash)
    }

    suspend fun dropStash(stash: StashListItem) {
        GitDownState.git.value.dropStash(stash)

        if (GitDownState.selectedStash.value?.sha == stash.sha) {
            GitDownState.selectedStash.value = null
        }
    }

}

// PATTERN: KOI-R-0020
// TITLE: codymikol/git-down / root / UserDirectoryRepository
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: real:codymikol/git-down@dfbb1e11eab9/src/main/kotlin/com/codymikol/repositories/UserDirectoryRepository.kt
// LICENSE: MIT
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: root/main/com/codymikol/repositories/UserDirectoryRepository.kt
package com.codymikol.repositories

import net.harawata.appdirs.AppDirs
import net.harawata.appdirs.AppDirsFactory
import org.koin.core.annotation.Single

@Single
open class UserDirectoryRepository {

    private val appDirs : AppDirs by lazy { AppDirsFactory.getInstance() }

    private val projectName = "gitdown"
    private val projectVersion = "0.0.0"
    private val author = "Cody Mikol"

    open fun getUserDataDir(): String? = appDirs.getUserDataDir(projectName, projectVersion, author)

}

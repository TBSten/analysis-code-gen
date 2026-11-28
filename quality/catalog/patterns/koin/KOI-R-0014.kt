// PATTERN: KOI-R-0014
// TITLE: codymikol/git-down / root / DiffToolService
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: real:codymikol/git-down@dfbb1e11eab9/src/main/kotlin/com/codymikol/services/DiffToolService.kt
// LICENSE: MIT
// STATUS: rejected
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので K（KSP2 2.3.14）: K でビルドが失敗した: Unresolved reference 'data'.
// FILE: root/main/com/codymikol/services/DiffToolService.kt
package com.codymikol.services

import com.codymikol.data.file.FileDelta
import com.codymikol.data.file.Status
import com.codymikol.state.GitDownState
import org.koin.core.annotation.Single
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

@Single
class DiffToolService {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(DiffToolService::class.java)
    }

    internal var launchProcess: (List<String>, File) -> Process = { command, workingDirectory ->
        ProcessBuilder(command).directory(workingDirectory).start()
    }

    fun getConfiguredDiffTool(): String? = GitDownState.repo.value.config.getString("diff", null, "tool")

    fun buildDiffToolCommand(fileDelta: FileDelta): List<String> {
        val command = mutableListOf("git", "difftool", "-y")
        getConfiguredDiffTool()?.let { command.addAll(listOf("-t", it)) }
        if (fileDelta.type == Status.INDEX) command.add("--cached")
        command.add("--")
        command.add(fileDelta.getPath())
        return command
    }

    fun launchDiffTool(fileDelta: FileDelta) {
        if (fileDelta.type == Status.STASH) {
            logger.warn("Launching an external diff tool for a stashed file is not currently supported")
            return
        }

        val command = buildDiffToolCommand(fileDelta)

        try {
            launchProcess(command, GitDownState.repo.value.workTree)
            logger.info("Launched diff tool for ${fileDelta.getPath()}: ${command.joinToString(" ")}")
        } catch (e: Exception) {
            logger.error("Failed to launch diff tool for ${fileDelta.getPath()}: ${e.message}")
        }
    }

}

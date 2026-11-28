@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.engine

import me.tbsten.analysis.code.gen.engine.spi.DuplicatePolicy
import me.tbsten.analysis.code.gen.engine.spi.OutputKind
import me.tbsten.analysis.code.gen.model.ExecutionUnitModel
import me.tbsten.analysis.code.gen.processor.Dependencies
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import java.io.File

/**
 * 出力トランザクション（ADR-014）。Stage（メモリ）→ Validate → Round Commit（staging に書く）→ Build Commit（出力ディレクトリと入れ替え）。
 *
 * 出力のキーは `<kotlin|java|resources>/<出力ルートからの相対パス>`。
 */
internal class OutputTransaction(
    private val rootDir: File,
    private val unit: ExecutionUnitModel,
    private val diagnostics: DiagnosticCollector,
) {
    class Entry(
        val kind: OutputKind,
        val relativePath: String,
        val content: ByteArray,
        val processorName: String,
        val round: Int,
        val dependencies: Dependencies,
    ) {
        val key: String get() = "${kind.dirName}/$relativePath"
    }

    /** Round Commit の結果。[added] は今回のラウンドで新しく増えた出力。 */
    class RoundCommit(
        val added: List<Entry>,
    )

    private val stagingRoot = File(rootDir, unit.stagingDir)
    private val journal = File(File(rootDir, unit.cacheDir), "commit-journal")
    private val committed = linkedMapOf<String, Entry>()
    private val staged = mutableListOf<Entry>()

    /** KSP 互換の重複検出用。Processor ごとの、これまでに書いたキー（ラウンドをまたぐ）。 */
    private val writtenBy = mutableMapOf<String, MutableSet<String>>()

    fun stagingDir(kind: OutputKind): File = File(stagingRoot, kind.dirName)

    /** staging 上のファイルの、rootDir からの相対パス。 */
    fun stagingPath(entry: Entry): String = File(stagingDir(entry.kind), entry.relativePath).relativeTo(rootDir).invariantSeparatorsPath

    private fun outputDir(kind: OutputKind): File =
        File(
            rootDir,
            when (kind) {
                OutputKind.Kotlin -> unit.outputDirs.kotlin
                OutputKind.Java -> unit.outputDirs.java
                OutputKind.Resource -> unit.outputDirs.resources
            },
        )

    fun prepare() {
        stagingRoot.deleteRecursively()
        stagingRoot.mkdirs()
    }

    /** 生成の呼び出し。パスの検査はここで行い、違反は呼び出し時点の [IllegalArgumentException]（API-06、TRM-06）。 */
    fun write(
        processorName: String,
        round: Int,
        kind: OutputKind,
        relativePath: String,
        content: ByteArray,
        dependencies: Dependencies,
        duplicatePolicy: DuplicatePolicy,
    ) {
        val path = checkRelativePath(relativePath)
        val entry = Entry(kind, path, content, processorName, round, dependencies)
        val written = synchronized(this) { writtenBy.getOrPut(processorName) { mutableSetOf() } }
        if (duplicatePolicy == DuplicatePolicy.ThrowKspException && entry.key in written) {
            // KSP の CodeGeneratorImpl と同じ例外（TRM-04 の KSP 側）
            throw FileAlreadyExistsException(File(stagingDir(kind), path), reason = "$processorName が同じファイルを 2 回作った")
        }
        synchronized(this) {
            written += entry.key
            staged += entry
        }
    }

    /**
     * ラウンドの Validate と Round Commit。重複は error を出して null を返す（その場合、このラウンドの生成物は staging に書かない）。
     */
    fun validateAndCommit(): RoundCommit? {
        val entries = synchronized(this) { staged.toList().also { staged.clear() } }
        val added = mutableListOf<Entry>()
        var failed = false
        for ((key, group) in entries.groupBy { it.key }) {
            val processors = group.map { it.processorName }.distinct()
            val previous = committed[key]
            when {
                processors.size > 1 -> {
                    failed = true
                    diagnostics.report(
                        Diagnostic.Severity.Error,
                        "異なる Processor が同じ出力先 $key に書いた: ${processors.joinToString(", ")}",
                        processors.first(),
                    )
                }
                group.size > 1 -> {
                    failed = true
                    diagnostics.report(
                        Diagnostic.Severity.Error,
                        "同じ Processor が同じラウンドで $key に ${group.size} 回書いた",
                        processors.single(),
                    )
                }
                previous != null && previous.processorName != processors.single() -> {
                    failed = true
                    diagnostics.report(
                        Diagnostic.Severity.Error,
                        "異なる Processor が同じ出力先 $key に書いた: ${previous.processorName}（ラウンド ${previous.round}）, " +
                            "${processors.single()}（ラウンド ${group.single().round}）",
                        processors.single(),
                    )
                }
                previous != null && previous.content.contentEquals(group.single().content) -> {
                    // 後のラウンドで同じ内容を書いたものは何もしない。進捗にも数えない（TRM-05）
                    diagnostics.report(
                        Diagnostic.Severity.Info,
                        "$key はラウンド ${previous.round} と同じ内容なので書き直さない",
                        processors.single(),
                    )
                }
                previous != null -> {
                    failed = true
                    diagnostics.report(
                        Diagnostic.Severity.Error,
                        "同じ Processor が $key をラウンド ${previous.round} と違う内容で書き直した",
                        processors.single(),
                    )
                }
                else -> added += group.single()
            }
        }
        if (failed) return null
        // 決定性のため、書く順もキーの辞書順にする
        for (entry in added.sortedBy { it.key }) {
            val file = File(stagingDir(entry.kind), entry.relativePath)
            file.parentFile.mkdirs()
            file.writeBytes(entry.content)
            committed[entry.key] = entry
        }
        return RoundCommit(added.sortedBy { it.key })
    }

    /**
     * Build Commit: journal → 出力ディレクトリを staging と入れ替える → journal を消す。
     * 出力ディレクトリにあった手置きのファイルは入れ替えで消える（OUT-05）。戻り値は出力の rootDir 相対パス（辞書順）。
     */
    fun buildCommit(): List<String> {
        journal.parentFile.mkdirs()
        journal.writeText("commit-start ${unit.name}\n")
        for (kind in OutputKind.entries) {
            val out = outputDir(kind)
            out.deleteRecursively()
            val staging = stagingDir(kind)
            out.parentFile?.mkdirs()
            if (staging.isDirectory && !staging.renameTo(out)) {
                staging.copyRecursively(out, overwrite = true)
                staging.deleteRecursively()
            }
            out.mkdirs()
        }
        stagingRoot.deleteRecursively()
        journal.delete()
        return committed.values
            .map { File(outputDir(it.kind), it.relativePath).relativeTo(rootDir).invariantSeparatorsPath }
            .sorted()
    }

    /** 失敗したとき。staging を捨て、出力ディレクトリは前回のまま（FAIL-01・02）。 */
    fun discard() {
        stagingRoot.deleteRecursively()
    }

    companion object {
        private val DRIVE = Regex("^[A-Za-z]:")

        /** 出力ルートの外を指すパス（`..`、絶対パス、空の区切り）を拒否する。 */
        fun checkRelativePath(path: String): String {
            require(path.isNotEmpty()) { "出力先のパスが空" }
            require(!path.startsWith("/") && !path.startsWith("\\") && !DRIVE.containsMatchIn(path)) {
                "出力ルートの外（絶対パス）には書けない: $path"
            }
            require('\\' !in path) { "パスの区切りは / で書く: $path" }
            val segments = path.split('/')
            require(segments.none { it == ".." }) { "出力ルートの外（..）には書けない: $path" }
            require(segments.none { it.isEmpty() || it == "." }) { "パスに空の区切りや . は使えない: $path" }
            return path
        }

        val OutputKind.dirName: String
            get() =
                when (this) {
                    OutputKind.Kotlin -> "kotlin"
                    OutputKind.Java -> "java"
                    OutputKind.Resource -> "resources"
                }
    }
}

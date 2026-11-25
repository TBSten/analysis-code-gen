package me.tbsten.analysis.code.gen.testfoundation

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 実行時の切り替え。
 *
 * - `acg.runPending=true`（Gradle では `-Pacg.runPending=true`）: 保留中のケースも中身を実行する（夜間の強制実行）。
 *   失敗してもビルドは落とさない。結果は `acg.pendingResultsDir`（acg.tests では `build/acg/`）の下に書く:
 *   - `pending-runs/<ID>__<hash>.tsv`: 実行した保留ケースすべて（`id\toutcome\tspec\ttitle\tmessage`、outcome = passed / failed）
 *   - `unexpected-pass/<ID>__<hash>.txt`: 予想外に通ったものだけ（`ID\tテスト名`）。夜間 CI（ACG-28、ADR-026）が集めて issue にする
 */
object CaseRuntime {
    /** テストからだけ上書きする。null ならシステムプロパティを読む。 */
    @Volatile
    var runPendingOverride: Boolean? = null

    val runPending: Boolean
        get() = runPendingOverride ?: System.getProperty("acg.runPending").toBoolean()

    /** テストからだけ上書きする。null ならシステムプロパティを読む。 */
    @Volatile
    var pendingResultsDirOverride: Path? = null

    val pendingResultsDir: Path?
        get() = pendingResultsDirOverride ?: System.getProperty("acg.pendingResultsDir")?.takeIf { it.isNotBlank() }?.let(Paths::get)

    /** 保留中のケースを強制実行した結果を書く。 */
    internal fun recordPendingRun(
        declaration: CaseDeclaration,
        error: Throwable?,
    ) {
        val dir = pendingResultsDir ?: return
        val base = "${declaration.id}__${Integer.toHexString((declaration.specClass + declaration.title).hashCode())}"
        val outcome = if (error == null) "passed" else "failed"
        val message = error?.let { "${it::class.java.name}: ${it.message}" }.orEmpty()
        write(dir.resolve("pending-runs/$base.tsv"), listOf(declaration.id, outcome, declaration.specClass, declaration.title, message))
        if (error == null) write(dir.resolve("unexpected-pass/$base.txt"), listOf(declaration.id, declaration.testName))
    }

    private fun write(
        file: Path,
        columns: List<String>,
    ) {
        Files.createDirectories(file.parent)
        Files.writeString(file, columns.joinToString("\t") { it.replace(Regex("[\t\r\n]+"), " ") } + "\n")
    }
}

package me.tbsten.analysis.code.gen.testfoundation

import io.kotest.core.test.TestScope
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.zip.CRC32
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 実行時の切り替え。
 *
 * - `acg.runPending=true`（Gradle では `-Pacg.runPending=true`）: 保留中のケースも中身を実行する（夜間の強制実行）。
 *   失敗してもビルドは落とさない。結果は `acg.pendingResultsDir`（acg.tests では `build/acg/`）の下に書く:
 *   - `pending-runs/<ID>__<hash>.tsv`: 実行した保留ケースすべて
 *     （`id\toutcome\tspec\ttitle\tmessage\tミリ秒`、outcome = passed / failed / timeout）
 *   - `unexpected-pass/<ID>__<hash>.txt`: 予想外に通ったものだけ（`ID\tテスト名`）。夜間 CI（ACG-28、ADR-026）が集めて issue にする
 * - `acg.pendingTimeoutSeconds`（既定 300、0 で無効）: 保留ケース 1 件の持ち時間。未実装の中身が止まっても夜間の job 全体を
 *   食い潰さないよう、超えたら timeout として記録して次へ進む（ADR-057）。有効なケースには効かない
 * - `acg.pendingShard=i/n`: 保留ケースを ID の CRC32 % n で n 分割し、i 番目だけを実行する（夜間の並列実行、ADR-057）。
 *   指定したときは有効なケースを実行しない（有効なケースは夜間の matrix job が受け持つ）
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

    /** テストからだけ上書きする。null ならシステムプロパティを読む。 */
    @Volatile
    var pendingTimeoutOverride: Duration? = null

    /** 保留ケース 1 件の持ち時間。null なら制限しない。 */
    val pendingTimeout: Duration?
        get() =
            pendingTimeoutOverride
                ?: (System.getProperty("acg.pendingTimeoutSeconds")?.trim()?.toLongOrNull() ?: DEFAULT_PENDING_TIMEOUT_SECONDS)
                    .takeIf { it > 0 }
                    ?.seconds

    /** テストからだけ上書きする。null ならシステムプロパティを読む。 */
    @Volatile
    var pendingShardOverride: Pair<Int, Int>? = null

    /** 保留ケースの分割（i 番目, n 分割）。null なら分割しない。 */
    val pendingShard: Pair<Int, Int>?
        get() = pendingShardOverride ?: System.getProperty("acg.pendingShard")?.takeIf { it.isNotBlank() }?.let(::parseShard)

    internal fun parseShard(text: String): Pair<Int, Int> {
        val parts = text.trim().split('/')
        val i = parts.getOrNull(0)?.toIntOrNull()
        val n = parts.getOrNull(1)?.toIntOrNull()
        require(parts.size == 2 && i != null && n != null && n > 0 && i in 0 until n) { "acg.pendingShard は i/n（0 <= i < n）で書く: '$text'" }
        return i to n
    }

    /** ケース [id] が分割 [shard] に入るか。ID の CRC32 % n で決める（実行順や追加に左右されない）。 */
    internal fun inShard(
        id: String,
        shard: Pair<Int, Int>,
    ): Boolean {
        val crc = CRC32().apply { update(id.toByteArray(Charsets.UTF_8)) }
        return (crc.value % shard.second).toInt() == shard.first
    }

    /**
     * 保留ケースの中身を持ち時間つきで実行し、失敗（時間切れを含む）を返す。
     *
     * 中身はブロッキングな処理（TestKit・サブプロセス）が多く、コルーチンの取り消しでは止まらない。
     * そこで別の daemon スレッドで動かし、持ち時間を過ぎたら割り込みをかけて待つのをやめる（スレッドは捨てる）。
     */
    internal fun runPendingBody(
        scope: TestScope,
        test: suspend TestScope.() -> Unit,
    ): Throwable? {
        val timeout = pendingTimeout
        val result = CompletableFuture<Unit>()
        val worker =
            Thread({
                try {
                    runBlocking { scope.test() }
                    result.complete(Unit)
                } catch (e: Throwable) {
                    result.completeExceptionally(e)
                }
            }, "acg-pending-run").apply {
                isDaemon = true
                start()
            }
        return try {
            if (timeout == null) result.get() else result.get(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            null
        } catch (e: TimeoutException) {
            worker.interrupt()
            PendingTimeoutException(timeout!!)
        } catch (e: ExecutionException) {
            val cause = e.cause ?: e
            if (cause is VirtualMachineError) throw cause
            cause
        }
    }

    /** 保留中のケースを強制実行した結果を書く。 */
    internal fun recordPendingRun(
        declaration: CaseDeclaration,
        error: Throwable?,
        elapsedMillis: Long = 0,
    ) {
        val dir = pendingResultsDir ?: return
        val base = "${declaration.id}__${Integer.toHexString((declaration.specClass + declaration.title).hashCode())}"
        val outcome =
            when (error) {
                null -> "passed"
                is PendingTimeoutException -> "timeout"
                else -> "failed"
            }
        val message = error?.let { "${it::class.java.name}: ${it.message}" }.orEmpty()
        write(
            dir.resolve("pending-runs/$base.tsv"),
            listOf(declaration.id, outcome, declaration.specClass, declaration.title, message, elapsedMillis.toString()),
        )
        if (error == null) write(dir.resolve("unexpected-pass/$base.txt"), listOf(declaration.id, declaration.testName))
    }

    const val DEFAULT_PENDING_TIMEOUT_SECONDS: Long = 300

    private fun write(
        file: Path,
        columns: List<String>,
    ) {
        Files.createDirectories(file.parent)
        Files.writeString(file, columns.joinToString("\t") { it.replace(Regex("[\t\r\n]+"), " ") } + "\n")
    }
}

/** 保留ケースが持ち時間（`acg.pendingTimeoutSeconds`）を超えた。 */
class PendingTimeoutException(
    timeout: Duration,
) : RuntimeException("保留ケースが持ち時間 $timeout を超えた（acg.pendingTimeoutSeconds）")

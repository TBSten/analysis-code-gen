package me.tbsten.analysis.code.gen.testfoundation.check

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Gradle から JavaExec で呼ぶ入口（acg.tests と :quality が使う）。
 *
 * - `collect --classes <dir>[:<dir>...] --out <file>`: テストのクラス出力から宣言を集める
 * - `check --manifests <file>[:...] --inventory <csv> --status <csv> --exceptions <csv> [--base-ref <git ref>] [--repo <dir>]
 *    [--require-all true|false] [--write true|false]`: 突き合わせて状態ファイルを検査（--write true なら書き換え）
 */
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val cmd = args.firstOrNull() ?: usage()
        val opts = args.drop(1).chunked(2).associate { (k, v) -> k.removePrefix("--") to v }
        val code =
            when (cmd) {
                "collect" -> collect(opts)
                "check" -> check(opts)
                else -> usage()
            }
        exitProcess(code)
    }

    private fun usage(): Nothing {
        System.err.println("usage: collect|check ...")
        exitProcess(2)
    }

    private fun paths(v: String?): List<Path> =
        v
            .orEmpty()
            .split(File.pathSeparator)
            .filter { it.isNotBlank() }
            .map(Paths::get)

    private fun collect(opts: Map<String, String>): Int {
        val result = CaseCollector.collectFromClassDirs(paths(opts["classes"]))
        val out = Paths.get(opts.getValue("out"))
        Files.createDirectories(out.parent)
        Files.writeString(out, CaseManifest.write(result.declarations, result.problems))
        println("ケース宣言 ${result.declarations.size} 本を集めた → $out")
        result.problems.forEach { System.err.println("問題: $it") }
        return 0
    }

    private fun check(opts: Map<String, String>): Int {
        val manifests = paths(opts["manifests"]).filter { Files.exists(it) }.map { CaseManifest.read(Files.readString(it)) }
        val statusPath = Paths.get(opts.getValue("status"))
        val exceptionsPath = opts["exceptions"]?.let(Paths::get)
        val write = opts["write"].toBoolean()
        val input =
            CaseChecker.Input(
                inventory = CaseChecker.parseInventory(Files.readString(Paths.get(opts.getValue("inventory")))),
                declarations = manifests.flatMap { it.declarations },
                collectProblems = manifests.flatMap { it.problems },
                baseStatus =
                    opts["base-ref"]
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            baseStatus(
                                it,
                                opts["repo"] ?: ".",
                            )
                        }?.let(CaseChecker::parseStatus),
                exceptions =
                    exceptionsPath
                        ?.takeIf {
                            Files.exists(
                                it,
                            )
                        }?.let { CaseChecker.parseExceptions(Files.readString(it)) }
                        .orEmpty(),
                requireAllCases = opts["require-all"].toBoolean(),
            )
        val report = CaseChecker.check(input)
        val generated = CaseChecker.formatStatus(report.rows)
        val errors = report.errors.toMutableList()
        if (write) {
            Files.writeString(statusPath, generated)
            println("状態ファイルを書いた → $statusPath")
        } else {
            val committed = if (Files.exists(statusPath)) Files.readString(statusPath) else ""
            if (committed != generated) {
                errors += "quality/case-status.csv がテストと合っていない。./gradlew :quality:updateCaseStatus を実行してコミットする"
            }
        }
        println(
            "ケース ${report.rows.size} 件: 有効 ${report.count(CaseChecker.Status.Enabled)} / 保留 ${report.count(CaseChecker.Status.Pending)}" +
                " / テスト無し ${report.count(CaseChecker.Status.Missing)}",
        )
        report.notices.forEach { println("お知らせ: $it") }
        errors.forEach { System.err.println("エラー: $it") }
        return if (errors.isEmpty()) 0 else 1
    }

    /** `git show <ref>:quality/case-status.csv`。取れなければ null（逆戻りの検査を飛ばす）。 */
    private fun baseStatus(
        ref: String,
        repo: String,
    ): String? =
        runCatching {
            val p =
                ProcessBuilder("git", "show", "$ref:quality/case-status.csv")
                    .directory(File(repo))
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
            val out = p.inputStream.bufferedReader().readText()
            if (p.waitFor(30, TimeUnit.SECONDS) && p.exitValue() == 0) out else null
        }.getOrNull()
}

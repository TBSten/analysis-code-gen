package poc.p02

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.system.measureNanoTime

/**
 * PoC P-02: ラウンド間で AA 環境を維持して生成物を差分反映する方式（KEEP）と、毎ラウンド作り直す方式（REBUILD）を比べる。
 *
 *   ./gradlew run --args="--files 200 --warmup 3 --iterations 10 --out results"
 *
 * 1. 正しさ: 各ラウンドの解析結果（Dumper のテキスト）が KEEP と REBUILD で一致するか。最終ラウンドでエラー 0 件か。
 * 2. 対照実験: KEEP からキャッシュ無効化を抜く（NO_INVALIDATE）と結果が変わるか（検証が無効化に敏感であることの確認）。
 * 3. 時間: ラウンド 2・3 の「差分反映 + 再解析」と「再構築 + 再解析」の中央値。
 */
enum class Mode { KEEP, REBUILD, NO_INVALIDATE, KEEP_OOB }

data class RoundResult(val round: Int, val transitionNanos: Long, val analyzeNanos: Long, val dump: Dump)

class Pipeline(private val work: Path, private val n: Int, private val profile: Profile, private val extraLibs: List<Path>) {
    private val src = work.resolve("src")
    private val gen = work.resolve("gen")
    private val stdlib: Path = Path.of(KotlinVersion::class.java.protectionDomain.codeSource.location.toURI())
    private val libs = listOf(stdlib) + extraLibs
    private val jdkHome: Path = Path.of(System.getProperty("java.home"))

    init {
        Scenario.writeSources(src, n)
        gen.createDirectories()
    }

    fun run(mode: Mode): List<RoundResult> {
        val results = mutableListOf<RoundResult>()
        var env: AaEnv
        var t = measureNanoTime { env = AaEnv(listOf(src), libs, jdkHome) }
        var targets = env.ktFiles.toList()
        var round = 1
        try {
            while (true) {
                lateinit var dump: Dump
                lateinit var written: List<Path>
                val a = measureNanoTime {
                    env.analyze {
                        dump = Dumper.dump(this, env.ktFiles, profile)
                        written = Scenario.process(this, round, targets, gen)
                    }
                }
                results += RoundResult(round, t, a, dump)
                if (written.isEmpty()) break
                round++
                if (round > 10) error("too many rounds")
                t = when (mode) {
                    Mode.KEEP -> measureNanoTime { env.addGeneratedFiles(written) }
                    Mode.NO_INVALIDATE -> measureNanoTime { env.addGeneratedFilesWithoutInvalidation(written) }
                    Mode.KEEP_OOB -> measureNanoTime { env.addGeneratedFilesWithModuleOutOfBlock(written) }
                    Mode.REBUILD -> measureNanoTime {
                        env.close()
                        env = AaEnv(listOf(src, gen), libs, jdkHome)
                    }
                }
                val writtenSet = written.map { it.absolute().normalize().toString() }.toSet()
                targets = env.ktFiles.filter { it.virtualFile.path in writtenSet }
                check(targets.size == written.size) { "round $round: targets ${targets.size} != written ${written.size}" }
            }
        } finally {
            env.close()
        }
        return results
    }
}

fun main(args: Array<String>) {
    val opts = args.toList().chunked(2).associate { it[0].removePrefix("--") to it.getOrElse(1) { "" } }
    val n = opts["files"]?.toInt() ?: 200
    val warmup = opts["warmup"]?.toInt() ?: 3
    val iterations = opts["iterations"]?.toInt() ?: 10
    val out = Path.of(opts["out"] ?: "results").createDirectories()
    // keepalive=true: IntelliJ Application 環境（とアプリ単位のライブラリ索引キャッシュ）をプロセス内で使い回す。
    // 設計 8.1 の「Application は Worker / Process 単位で再利用」に合わせた条件で、REBUILD に有利（保守的）。
    val keepalive = opts["keepalive"]?.toBoolean() ?: true
    if (keepalive) System.setProperty("kotlin.environment.keepalive", "true")
    val base = Files.createTempDirectory("poc-p02-")
    // minimal: kotlin-stdlib + JDK だけ。realistic: よくあるアプリ相当の依存（build.gradle.kts の benchLibs）も足す。
    val libsMode = opts["libs"] ?: "minimal"
    val extraLibs = when (libsMode) {
        "minimal" -> emptyList()
        "realistic" -> System.getProperty("p02.benchLibs").orEmpty().split(java.io.File.pathSeparator).filter { it.isNotBlank() }.map { Path.of(it) }
        else -> error("unknown --libs $libsMode")
    }
    val dumps = out.resolve("dumps").createDirectories()

    val report = StringBuilder()
    fun log(s: String) { println(s); report.append(s).append('\n') }
    log("# PoC P-02 result (files=$n, warmup=$warmup, iterations=$iterations, keepalive=$keepalive, libs=$libsMode(+${extraLibs.size} jars), java=${System.getProperty("java.version")})")

    // 1. 正しさ（両プロファイル）
    var seq = 0
    val checkProfiles = (opts["check"] ?: "DECL,FULL").split(',').filter { it.isNotBlank() }.map { Profile.valueOf(it) }
    for (profile in checkProfiles) {
        val runs = Mode.entries.associateWith { Pipeline(base.resolve("c${seq++}"), n, profile, extraLibs).run(it) }
        val keep = runs.getValue(Mode.KEEP)
        val rebuild = runs.getValue(Mode.REBUILD)
        val noInv = runs.getValue(Mode.NO_INVALIDATE)
        val oob = runs.getValue(Mode.KEEP_OOB)
        log("\n## correctness profile=$profile")
        log("rounds: KEEP=${keep.size} REBUILD=${rebuild.size} NO_INVALIDATE=${noInv.size}")
        for (i in rebuild.indices) {
            val r = rebuild[i]
            val k = keep.getOrNull(i)
            val ni = noInv.getOrNull(i)
            val same = k != null && k.dump.text == r.dump.text
            val niSame = ni != null && ni.dump.text == r.dump.text
            val oobSame = oob.getOrNull(i)?.dump?.text == r.dump.text
            log(
                "round ${r.round}: KEEP==REBUILD: $same | NO_INVALIDATE==REBUILD: $niSame | KEEP_OOB==REBUILD: $oobSame | " +
                    "errors(R/K)=${r.dump.errors}/${k?.dump?.errors} unresolved(R/K)=${r.dump.unresolved}/${k?.dump?.unresolved} " +
                    "dumpLines=${r.dump.text.lines().size}",
            )
            dumps.resolve("dump-$profile-round${r.round}-REBUILD.txt").writeText(r.dump.text)
            if (k != null) dumps.resolve("dump-$profile-round${r.round}-KEEP.txt").writeText(k.dump.text)
            if (ni != null && !niSame) dumps.resolve("dump-$profile-round${r.round}-NO_INVALIDATE.txt").writeText(ni.dump.text)
        }
    }

    // 2. 時間（--timing DECL,FULL で対象を選ぶ。FULL は遅いので既定は DECL だけ）
    val timingProfiles = (opts["timing"] ?: "DECL").split(',').filter { it.isNotBlank() }.map { Profile.valueOf(it) }
    for (profile in timingProfiles) {
        val samples = mutableMapOf<Pair<Mode, Int>, MutableList<Long>>()
        val transitions = mutableMapOf<Pair<Mode, Int>, MutableList<Long>>()
        repeat(warmup + iterations) { it ->
            for (mode in listOf(Mode.KEEP, Mode.REBUILD, Mode.KEEP_OOB)) {
                val res = Pipeline(base.resolve("t${seq++}"), n, profile, extraLibs).run(mode)
                if (it >= warmup) {
                    for (r in res.drop(1)) {
                        samples.getOrPut(mode to r.round) { mutableListOf() } += (r.transitionNanos + r.analyzeNanos)
                        transitions.getOrPut(mode to r.round) { mutableListOf() } += r.transitionNanos
                    }
                    samples.getOrPut(mode to 1) { mutableListOf() } += (res[0].transitionNanos + res[0].analyzeNanos)
                }
            }
        }
        log("\n## timing profile=$profile (median ms of transition+analysis; round 1 = initial build+analysis)")
        val rounds = samples.keys.map { it.second }.toSortedSet()
        for (round in rounds) {
            val k = median(samples[Mode.KEEP to round])
            val r = median(samples[Mode.REBUILD to round])
            val kt = median(transitions[Mode.KEEP to round])
            val rt = median(transitions[Mode.REBUILD to round])
            val o = median(samples[Mode.KEEP_OOB to round])
            log(
                "round $round: KEEP_OOB=${"%.1f".format(o)}ms(${"%.1f".format(100 * o / r)}%) KEEP=${"%.1f".format(k)}ms REBUILD=${"%.1f".format(r)}ms ratio=${"%.1f".format(100 * k / r)}%" +
                    (if (round > 1) "  (transition only: KEEP=${"%.1f".format(kt)}ms REBUILD=${"%.1f".format(rt)}ms)" else ""),
            )
        }
    }
    out.resolve("report-files$n-$libsMode-keepalive-$keepalive.md").writeText(report.toString())
    base.toFile().deleteRecursively()
    // IntelliJ のスレッドが残ることがあるので明示的に終える。
    kotlin.system.exitProcess(0)
}

private fun median(xs: List<Long>?): Double {
    if (xs.isNullOrEmpty()) return Double.NaN
    val s = xs.sorted()
    val mid = s.size / 2
    val v = if (s.size % 2 == 1) s[mid].toDouble() else (s[mid - 1] + s[mid]) / 2.0
    return v / 1_000_000.0
}

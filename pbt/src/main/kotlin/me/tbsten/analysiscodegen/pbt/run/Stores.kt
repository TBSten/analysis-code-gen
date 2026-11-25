package me.tbsten.analysiscodegen.pbt.run

import java.io.File
import java.time.Instant
import java.util.zip.CRC32

/** 1 試行あたりの時間の記録（性質ごとに 1 ファイル。並行するテスト JVM が同じファイルを書かない）。 */
public data class Timing(
    val name: String,
    val nanosPerTrial: Double,
    val samples: Long,
    val weight: Double,
    val updatedEpochMs: Long,
    /** 直近の実行にかかった時間 */
    val lastRunNanos: Long = 0,
    /** 試行数を調整しない（決定的な網羅層など）。予算から先に差し引く */
    val fixed: Boolean = false,
)

/**
 * タイミングの置き場（`<stateDir>/timing/<性質>.tsv`）。毎時のワークフローは actions/cache でこのディレクトリを持ち回る。
 * 7 日更新の無い記録（消えた性質）は配分の計算から外す。
 */
public class TimingStore(
    private val dir: File,
) {
    private fun file(name: String) = File(dir, "timing/${safe(name)}.tsv")

    public fun read(name: String): Timing? = parse(file(name))

    public fun all(nowEpochMs: Long = System.currentTimeMillis()): List<Timing> =
        File(dir, "timing")
            .listFiles { f -> f.extension == "tsv" }
            .orEmpty()
            .mapNotNull { parse(it) }
            .filter { nowEpochMs - it.updatedEpochMs < STALE_MS }

    /** 指数移動平均で更新する（直近の実行を重く見る）。 */
    public fun update(
        name: String,
        nanosPerTrial: Double,
        trials: Long,
        weight: Double,
        runNanos: Long = (nanosPerTrial * trials).toLong(),
        fixed: Boolean = false,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Timing {
        val old = read(name)
        val avg = if (old == null) nanosPerTrial else old.nanosPerTrial * (1 - EMA_ALPHA) + nanosPerTrial * EMA_ALPHA
        val t = Timing(name, avg, (old?.samples ?: 0) + trials, weight, nowEpochMs, runNanos, fixed)
        val f = file(name)
        f.parentFile.mkdirs()
        f.writeText(
            listOf(t.name, t.nanosPerTrial, t.samples, t.weight, t.updatedEpochMs, t.lastRunNanos, t.fixed).joinToString("\t") + "\n",
        )
        return t
    }

    private fun parse(f: File): Timing? {
        if (!f.isFile) return null
        val c = f.readText().trim().split('\t')
        if (c.size < 5) return null
        return Timing(
            name = c[0],
            nanosPerTrial = c[1].toDoubleOrNull() ?: return null,
            samples = c[2].toLongOrNull() ?: 0,
            weight = c[3].toDoubleOrNull() ?: 1.0,
            updatedEpochMs = c[4].toLongOrNull() ?: 0,
            lastRunNanos = c.getOrNull(5)?.toLongOrNull() ?: 0,
            fixed = c.getOrNull(6)?.toBoolean() ?: false,
        )
    }

    public companion object {
        public const val EMA_ALPHA: Double = 0.5
        public const val STALE_MS: Long = 7L * 24 * 60 * 60 * 1000
    }
}

/** 保存した反例（回帰ケース）1 件。 */
public data class Regression(
    val seed: Long,
    val iterations: Int,
    val generatorVersion: String,
    val note: String,
)

/**
 * 回帰ケースの置き場。テストのリソース `pbt/regressions/<性質>.tsv`（列: seed・試行数・生成器の版・メモ）。
 * PR ごとの実行で毎回再生する。生成器の版が今と違う行も再生はする（ただの固定入力として価値があるため）。
 */
public object RegressionStore {
    public fun load(
        name: String,
        loader: ClassLoader = Thread.currentThread().contextClassLoader,
    ): List<Regression> {
        val text = loader.getResource("pbt/regressions/${safe(name)}.tsv")?.readText() ?: return emptyList()
        return parse(text)
    }

    public fun parse(text: String): List<Regression> =
        text
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val c = line.split('\t')
                val seed = c.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
                Regression(seed, c.getOrNull(1)?.toIntOrNull() ?: 1, c.getOrNull(2).orEmpty(), c.getOrNull(3).orEmpty())
            }.toList()

    public fun format(r: Regression): String = "${r.seed}\t${r.iterations}\t${r.generatorVersion}\t${r.note.replace('\t', ' ')}"
}

/** 失敗の書き出し（ワークフローが artifact と issue にする）。 */
public object FailureReports {
    public fun write(
        settings: PbtSettings,
        name: String,
        regression: Regression,
        message: String,
    ): File {
        val dir = File(settings.reportDir, "failures").apply { mkdirs() }
        File(dir, "new-regressions.tsv").appendText("${safe(name)}\t${RegressionStore.format(regression)}\n")
        val f = File(dir, "${safe(name)}-${regression.seed}.md")
        f.writeText(
            buildString {
                appendLine("# PBT の反例: $name")
                appendLine()
                appendLine("- 日時: ${Instant.now()}")
                appendLine("- seed: ${regression.seed}（試行数 ${regression.iterations}、生成器 ${regression.generatorVersion}）")
                appendLine("- 再現: `${reproduceCommand(name, regression)}`")
                appendLine("- 回帰ケースにする: `pbt/regressions/${safe(name)}.tsv` に次の 1 行を足す")
                appendLine()
                appendLine("```")
                appendLine(RegressionStore.format(regression))
                appendLine("```")
                appendLine()
                appendLine("## 縮小後の反例とメッセージ")
                appendLine()
                appendLine("```")
                appendLine(message)
                appendLine("```")
            },
        )
        return f
    }

    public fun reproduceCommand(
        name: String,
        r: Regression,
    ): String = "./gradlew test -Pacg.pbt.only='$name' -Pacg.pbt.seed=${r.seed} -Pacg.pbt.iterations=${r.iterations}"
}

/** 生成した値のラベルを数える（網羅の記録。`build/pbt/coverage/<性質>.tsv`）。 */
public class LabelCounter {
    private val counts = LinkedHashMap<String, Long>()

    public fun add(labels: Iterable<String>) {
        for (l in labels) counts[l] = (counts[l] ?: 0) + 1
    }

    public fun snapshot(): Map<String, Long> = counts.toMap()

    public fun write(
        settings: PbtSettings,
        name: String,
    ) {
        if (counts.isEmpty()) return
        val f = File(settings.reportDir, "coverage/${safe(name)}.tsv")
        f.parentFile.mkdirs()
        f.writeText(counts.entries.sortedBy { it.key }.joinToString("") { "${it.key}\t${it.value}\n" })
    }
}

/** 決定的な網羅層の区画。ID の CRC32 % 24（カタログの `catalog.py` と同じ式）。 */
public object Shards {
    public fun of(
        id: String,
        shards: Int = SHARDS,
    ): Int {
        val c = CRC32()
        c.update(id.toByteArray(Charsets.UTF_8))
        return (c.value % shards).toInt()
    }
}

internal fun safe(name: String): String = name.replace(Regex("[^A-Za-z0-9._-]"), "_")

package me.tbsten.analysiscodegen.pbt.run

import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.PropertyContext
import io.kotest.property.checkAll
import java.util.zip.CRC32
import kotlin.time.Duration.Companion.nanoseconds

/** 1 回の実行のまとめ。テストから試行数や再生した回帰を確かめるのに使う。 */
public data class PbtRunSummary(
    val name: String,
    val skipped: Boolean,
    val batches: List<PbtBatch>,
    val labels: Map<String, Long>,
) {
    val trials: Int get() = batches.sumOf { it.iterations }
}

public data class PbtBatch(
    val seed: Long,
    val iterations: Int,
    val origin: String,
    val nanos: Long,
)

/** 失敗をまとめた例外。[regression] をそのまま回帰ケースのファイルに足せば、PR ごとに再生される。 */
public class PbtFailure(
    public val property: String,
    public val regression: Regression,
    message: String,
    cause: Throwable,
) : AssertionError(message, cause)

/**
 * このプロジェクトの PBT の入口（ADR-022）。kotest の `checkAll` を、seed・回帰・試行数の自動調整で包む。
 *
 * 1. `acg.pbt.seed` があれば、その seed だけを回す（再現）
 * 2. そうでなければ、保存した回帰 seed（`pbt/regressions/<name>.tsv`）を全部再生する
 * 3. PR モード: 性質ごとに固定の seed で [prIterations] 回（毎回同じ入力）
 *    定期実行モード: 新しい seed で、持ち分の時間に収まるまでバッチを重ねる（[IterationPlanner]）
 *
 * 失敗したら kotest が縮小した反例と seed を、`build/pbt/failures/` に書き出し、再現コマンド付きの [PbtFailure] を投げる。
 *
 * @param name 性質の名前。テストケースの ID を先頭に付ける（例: `PBT-01 生成したプロジェクトはコンパイルできる`）
 * @param generatorVersion 生成器の版。保存した seed がどの版の生成器で見つかったかを残す
 * @param weight 定期実行の時間の配分の重み
 * @param labels 生成した値のラベル（網羅の記録用）
 * @param regressions 再生する回帰ケース（既定はテストのリソース `pbt/regressions/<name>.tsv`）
 */
public suspend fun <A> acgCheckAll(
    name: String,
    arb: Arb<A>,
    generatorVersion: String = "1",
    prIterations: Int = 20,
    weight: Double = 1.0,
    labels: ((A) -> Iterable<String>)? = null,
    settings: PbtSettings = PbtSettings.current,
    regressions: List<Regression> = RegressionStore.load(name),
    property: suspend PropertyContext.(A) -> Unit,
): PbtRunSummary {
    if (settings.only != null && settings.only != name) return PbtRunSummary(name, true, emptyList(), emptyMap())
    val counter = LabelCounter()
    val batches = mutableListOf<PbtBatch>()

    suspend fun batch(
        seed: Long,
        iterations: Int,
        origin: String,
    ): PbtBatch {
        val start = System.nanoTime()
        try {
            checkAll(PropTestConfig(seed = seed, iterations = iterations), arb) { a ->
                labels?.let { counter.add(it(a)) }
                property(a)
            }
        } catch (e: Throwable) {
            if (e is PbtFailure) throw e
            val r =
                Regression(seed, iterations, generatorVersion, "$origin: ${e.message?.lineSequence()?.firstOrNull().orEmpty().take(80)}")
            val report = FailureReports.write(settings, name, r, e.message.orEmpty())
            throw PbtFailure(
                name,
                r,
                buildString {
                    appendLine("PBT「$name」が失敗した（$origin、seed=$seed、試行数 $iterations）")
                    appendLine("再現: ${FailureReports.reproduceCommand(name, r)}")
                    appendLine("回帰ケースにする: src/test/resources/pbt/regressions/${safe(name)}.tsv に「${RegressionStore.format(r)}」を足す")
                    appendLine("記録: ${report.path}")
                    append(e.message)
                },
                e,
            )
        }
        return PbtBatch(seed, iterations, origin, System.nanoTime() - start).also { batches += it }
    }

    val explicit = settings.seed
    if (explicit != null) {
        batch(explicit, settings.iterations ?: prIterations, "explicit")
    } else {
        for (r in regressions) batch(r.seed, r.iterations, "regression")
        when (settings.mode) {
            PbtMode.PR -> batch(prSeed(name), settings.iterations ?: prIterations, "pr")
            PbtMode.SCHEDULED -> scheduled(name, weight, settings) { seed, n -> batch(seed, n, "scheduled") }
        }
    }
    counter.write(settings, name)
    return PbtRunSummary(name, false, batches.toList(), counter.snapshot())
}

private suspend fun scheduled(
    name: String,
    weight: Double,
    settings: PbtSettings,
    run: suspend (seed: Long, iterations: Int) -> PbtBatch,
) {
    val timings = TimingStore(settings.stateDir)
    val planner = IterationPlanner(settings, timings)
    val share = planner.share(name, weight)
    val base = settings.runStartEpochMs xor System.nanoTime()
    val start = System.nanoTime()
    var perTrial: Double? = timings.read(name)?.nanosPerTrial
    var previous = 0
    var total = 0L
    var index = 0L
    while (true) {
        val n =
            settings.iterations?.takeIf { previous == 0 }
                ?: planner.nextBatch(share, (System.nanoTime() - start).nanoseconds, perTrial, previous)
        if (n <= 0 || (settings.iterations != null && previous != 0)) break
        val b = run(splitMix(base + index++), n)
        val measured = b.nanos.toDouble() / n
        perTrial = if (previous == 0 && perTrial == null) measured else (perTrial ?: measured) * 0.5 + measured * 0.5
        previous = n
        total += n
    }
    val elapsed = System.nanoTime() - start
    if (total > 0) timings.update(name, elapsed.toDouble() / total, total, weight, runNanos = elapsed)
}

/**
 * 決定的な網羅層: 値の一覧（カタログのパターンなど）から今の区画のものだけを、縮小なしで 1 回ずつ回す。
 * 区画は [Shards.of]（ID の CRC32 % 24）。PR モードでは [prSample] 件だけを ID 順に回す。
 * 所要時間は「固定」として記録し、定期実行の予算から先に差し引く。
 */
public suspend fun <A> acgCheckExhaustive(
    name: String,
    values: List<A>,
    id: (A) -> String,
    prSample: Int = 5,
    settings: PbtSettings = PbtSettings.current,
    property: suspend (A) -> Unit,
): PbtRunSummary {
    if (settings.only != null && settings.only != name) return PbtRunSummary(name, true, emptyList(), emptyMap())
    val targets =
        when {
            settings.mode == PbtMode.SCHEDULED -> values.filter { Shards.of(id(it)) == settings.shard }
            else -> values.sortedBy(id).take(prSample)
        }
    val counter = LabelCounter()
    val start = System.nanoTime()
    for (v in targets) {
        counter.add(listOf(id(v)))
        try {
            property(v)
        } catch (e: Throwable) {
            throw AssertionError("網羅層「$name」がパターン ${id(v)} で失敗した（区画 ${settings.shard}）\n${e.message}", e)
        }
    }
    val elapsed = System.nanoTime() - start
    if (settings.mode == PbtMode.SCHEDULED && targets.isNotEmpty()) {
        TimingStore(
            settings.stateDir,
        ).update(name, elapsed.toDouble() / targets.size, targets.size.toLong(), 0.0, runNanos = elapsed, fixed = true)
    }
    counter.write(settings, name)
    return PbtRunSummary(name, false, listOf(PbtBatch(settings.shard.toLong(), targets.size, "exhaustive", elapsed)), counter.snapshot())
}

/** PR モードの固定 seed（性質の名前から決まる）。 */
public fun prSeed(name: String): Long {
    val c = CRC32()
    c.update(name.toByteArray(Charsets.UTF_8))
    return splitMix(c.value)
}

internal fun splitMix(x: Long): Long {
    var z = x + -0x61c8864680b583ebL
    z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
    z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
    return (z xor (z ushr 31)) and Long.MAX_VALUE
}

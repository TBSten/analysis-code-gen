package me.tbsten.analysiscodegen.pbt.run

import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds

/**
 * 定期実行で、各性質に使ってよい時間（持ち分）と、次のバッチの試行数を決める（ADR-022 の 6 節）。
 *
 * - 持ち分 = (予算 − 決定的な網羅層の直近の所要時間の合計) × 自分の重み / 全性質の重みの合計
 * - 「全性質」は直近 7 日にタイミングの記録がある性質。初めて回る性質は記録が無いので、持ち分を [FIRST_RUN_CAP] で抑える
 *   （次の実行からは全員の記録がそろい、合計がほぼ予算になる）
 * - 1 試行の時間はバッチごとに計測して更新し、残りの持ち分 ÷ 1 試行の時間 で次のバッチを決める（倍々で増やす）
 * - 上限（既定 40 分）を過ぎたら、持ち分が残っていても新しいバッチを始めない
 */
public class IterationPlanner(
    private val settings: PbtSettings,
    private val timings: TimingStore,
) {
    public fun share(
        name: String,
        weight: Double,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Duration {
        val all = timings.all(nowEpochMs).filter { it.name != name }
        val fixed = all.filter { it.fixed }.sumOf { it.lastRunNanos }.nanoseconds
        val adaptive = all.filter { !it.fixed }
        val totalWeight = adaptive.sumOf { it.weight } + weight
        val pool = (settings.budget - fixed).coerceAtLeast(settings.budget / 4)
        val s = pool * (weight / totalWeight)
        val known = timings.read(name) != null
        return if (known) s else minOf(s, FIRST_RUN_CAP)
    }

    /**
     * 次のバッチの試行数。0 なら打ち切る。
     * @param elapsed この性質で既に使った時間
     * @param nanosPerTrial これまでの計測（無ければ null）
     * @param previous 直前のバッチの試行数（初回は 0）
     */
    public fun nextBatch(
        share: Duration,
        elapsed: Duration,
        nanosPerTrial: Double?,
        previous: Int,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Int {
        if (nowEpochMs >= settings.deadlineEpochMs) return 0
        if (previous == 0) return CALIBRATION
        val perTrial = nanosPerTrial ?: return 0
        val remaining = min((share - elapsed).inWholeNanoseconds, (settings.deadlineEpochMs - nowEpochMs) * 1_000_000)
        if (remaining <= 0) return 0
        val est = (remaining / max(perTrial, 1.0)).toLong()
        if (est < 1) return 0
        return min(min(est, max(previous.toLong() * 4, CALIBRATION.toLong())), MAX_BATCH.toLong()).toInt()
    }

    public companion object {
        /** 最初のバッチ（1 試行の時間を測る） */
        public const val CALIBRATION: Int = 10

        /** 1 バッチの上限（反例の再現に要る試行数を抑える） */
        public const val MAX_BATCH: Int = 2_000

        /** 記録の無い性質の持ち分の上限 */
        public val FIRST_RUN_CAP: Duration = 2.minutes
    }
}

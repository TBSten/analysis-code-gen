package me.tbsten.analysis.code.gen.pbt.run

import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/** PBT の実行の仕方（ADR-022 の 6 節）。 */
public enum class PbtMode {
    /** PR ごと: 保存した回帰 seed + 固定 seed で少ない試行。毎回同じ結果になる */
    PR,

    /** 毎時の定期実行: 回帰 seed + 新しい seed で、予算（既定 30 分）に収まる試行数 */
    SCHEDULED,
}

/**
 * PBT の設定。system property（Gradle の `-Pacg.pbt.*` から acg.jvm が渡す）→ 環境変数（`ACG_PBT_*`）→ 既定値の順で読む。
 *
 * | キー | 既定 | 意味 |
 * |---|---|---|
 * | `acg.pbt.mode` | `pr` | `pr` / `scheduled` |
 * | `acg.pbt.seed` | なし | 指定するとその seed だけで回す（再現用。回帰 seed は回さない） |
 * | `acg.pbt.iterations` | なし | 試行数を固定する（自動調整を止める） |
 * | `acg.pbt.only` | なし | この名前の性質だけを回す（再現用） |
 * | `acg.pbt.budgetMinutes` | 30 | 定期実行の予算（全性質の合計） |
 * | `acg.pbt.hardLimitMinutes` | 40 | これを過ぎたら新しい試行を始めない |
 * | `acg.pbt.runStartEpochMs` | テスト JVM の起動時刻 | 予算を数え始める時刻。ワークフローがビルド開始時刻を渡す |
 * | `acg.pbt.shard` | 現在の UTC の時 | 決定的な網羅層の区画（0〜23） |
 * | `acg.pbt.stateDir` | `<root>/.pbt-state` | 1 試行あたりの時間の記録（タイミング）の置き場 |
 * | `acg.pbt.reportDir` | `build/pbt` | 失敗の記録・網羅の記録の書き出し先 |
 * | `kotest.framework.timeout` | 600000 | kotest の 1 テストの時間切れ（ミリ秒）。持ち分はこれより短く抑える（ADR-046） |
 */
public data class PbtSettings(
    val mode: PbtMode,
    val seed: Long?,
    val iterations: Int?,
    val only: String?,
    val budget: Duration,
    val hardLimit: Duration,
    val runStartEpochMs: Long,
    val shard: Int,
    val stateDir: File,
    val reportDir: File,
    val testTimeout: Duration = KOTEST_DEFAULT_TIMEOUT,
) {
    public val deadlineEpochMs: Long get() = runStartEpochMs + hardLimit.inWholeMilliseconds
    public val budgetEndEpochMs: Long get() = runStartEpochMs + budget.inWholeMilliseconds

    public companion object {
        private val jvmStart = System.currentTimeMillis()

        public val current: PbtSettings by lazy { fromEnvironment() }

        public fun fromEnvironment(get: (String) -> String? = ::lookup): PbtSettings {
            val mode = if (get("acg.pbt.mode")?.lowercase() == "scheduled") PbtMode.SCHEDULED else PbtMode.PR
            val start = get("acg.pbt.runStartEpochMs")?.toLongOrNull() ?: jvmStart
            val shard =
                get("acg.pbt.shard")?.toIntOrNull()?.mod(SHARDS)
                    ?: Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).hour % SHARDS
            return PbtSettings(
                mode = mode,
                seed = get("acg.pbt.seed")?.toLongOrNull(),
                iterations = get("acg.pbt.iterations")?.toIntOrNull(),
                only = get("acg.pbt.only")?.takeIf { it.isNotBlank() },
                budget = (get("acg.pbt.budgetMinutes")?.toDoubleOrNull() ?: 30.0).let { (it * 60_000).toLong().milliseconds },
                hardLimit = (get("acg.pbt.hardLimitMinutes")?.toDoubleOrNull() ?: 40.0).let { (it * 60_000).toLong().milliseconds },
                runStartEpochMs = start,
                shard = shard,
                stateDir = File(get("acg.pbt.stateDir") ?: ".pbt-state"),
                reportDir = File(get("acg.pbt.reportDir") ?: "build/pbt"),
                testTimeout = get("kotest.framework.timeout")?.toLongOrNull()?.milliseconds ?: KOTEST_DEFAULT_TIMEOUT,
            )
        }

        private fun lookup(key: String): String? = System.getProperty(key) ?: System.getenv(key.uppercase().replace('.', '_'))
    }
}

/** 決定的な網羅層の区画の数（24 時間で一巡）。カタログの index.json の `shards` と同じ。 */
public const val SHARDS: Int = 24

/** kotest の 1 テストの時間切れの既定（kotest 6 の既定値と同じ 10 分）。 */
public val KOTEST_DEFAULT_TIMEOUT: Duration = 10.minutes

/** 既定の予算。 */
public val DEFAULT_BUDGET: Duration = 30.minutes

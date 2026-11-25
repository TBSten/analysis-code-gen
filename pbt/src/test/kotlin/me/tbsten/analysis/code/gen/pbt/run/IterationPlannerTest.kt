package me.tbsten.analysis.code.gen.pbt.run

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class IterationPlannerTest : FreeSpec({
    fun setup(): Pair<PbtSettings, TimingStore> {
        val dir = Files.createTempDirectory("planner").toFile()
        val s =
            PbtSettings
                .fromEnvironment { k -> if (k == "acg.pbt.stateDir") File(dir, "s").path else null }
                .copy(mode = PbtMode.SCHEDULED, runStartEpochMs = System.currentTimeMillis())
        return s to TimingStore(s.stateDir)
    }

    "記録の無い性質の持ち分は 2 分まで" {
        val (s, store) = setup()
        IterationPlanner(s, store).share("new", 1.0) shouldBe IterationPlanner.FIRST_RUN_CAP
    }

    "持ち分は重みで割り、決定的な網羅層の所要時間を先に引く" {
        val (s, store) = setup()
        store.update("a", 1e6, 10, 1.0)
        store.update("b", 1e6, 10, 2.0)
        store.update("exhaustive", 1e6, 10, 0.0, runNanos = 6.minutes.inWholeNanoseconds, fixed = true)
        val p = IterationPlanner(s, store)
        // (30 分 - 6 分) × 1 / (1 + 2) = 8 分
        p.share("a", 1.0) shouldBe 8.minutes
        p.share("b", 2.0) shouldBe 16.minutes
    }

    "次のバッチは残り時間 ÷ 1 試行の時間で、倍々までしか増やさない" {
        val (s, store) = setup()
        val p = IterationPlanner(s, store)
        p.nextBatch(10.seconds, 0.seconds, null, 0) shouldBe IterationPlanner.CALIBRATION
        // 1 試行 1ms・残り 10 秒 → 10,000 回だが、前回 10 の 4 倍 = 40
        p.nextBatch(10.seconds, 0.seconds, 1e6, 10) shouldBe 40
        // 残り 0.1 秒 → 100 回
        p.nextBatch(10.seconds, 9.9.seconds, 1e6, 1000) shouldBe 100
        p.nextBatch(10.seconds, 11.seconds, 1e6, 1000) shouldBe 0
    }

    "上限時刻を過ぎたら新しいバッチを始めない" {
        val (s, store) = setup()
        val late = s.copy(runStartEpochMs = System.currentTimeMillis() - 41.minutes.inWholeMilliseconds)
        IterationPlanner(late, store).nextBatch(10.minutes, 0.seconds, 1e6, 0) shouldBe 0
    }

    "区画は ID の CRC32 % 24（カタログの catalog.py と同じ）" {
        // python3 -c "import zlib; print(zlib.crc32(b'CMN-O-0001') % 24)" → 18
        Shards.of("CMN-O-0001") shouldBe 18
    }

    "既定の区画は UTC の時" {
        val s = PbtSettings.fromEnvironment { k -> if (k == "acg.pbt.runStartEpochMs") "${5 * 3_600_000L + 1}" else null }
        s.shard shouldBe 5
    }
})

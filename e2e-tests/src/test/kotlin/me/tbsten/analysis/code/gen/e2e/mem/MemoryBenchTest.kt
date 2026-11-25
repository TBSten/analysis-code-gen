package me.tbsten.analysis.code.gen.e2e.mem

import io.kotest.assertions.withClue
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import me.tbsten.analysis.code.gen.e2e.support.SampleEngine
import me.tbsten.analysis.code.gen.e2e.support.benchRunner
import me.tbsten.analysis.code.gen.e2e.support.copyFixtureDir
import me.tbsten.analysis.code.gen.e2e.support.generateLarge
import me.tbsten.analysis.code.gen.e2e.support.heap
import me.tbsten.analysis.code.gen.e2e.support.stopDaemons
import me.tbsten.analysis.code.gen.testfoundation.BenchSpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase

// 5.24 メモリ（MEM）のうち種別 B のもの。ヒープは初期化スクリプトの acgHeap が daemon の中で測る（ADR-037）
// Worker の既定（ClassLoader 分離）では Processor と AA は daemon の中で動くので、daemon のヒープで見られる

class MemoryBenchTest : BenchSpec({
    case("MEM-02", "同じ Gradle daemon で 20 回続けてビルドしても、ヒープが増え続けない", pending = PendingCase(Milestone.M2)) {
        val dir = copyFixtureDir("jvm")
        stopDaemons(dir)
        // 毎回 clean して全件処理し、GC 後の使用量を記録する
        val used =
            List(20) {
                benchRunner(dir, SampleEngine.Native, "clean", "acgCodegen", "acgHeap").build().heap().first
            }
        // 増え続けない = 立ち上がり（最初の 5 回）のあとは頭打ち。20 回目が 5 回目の 1.1 倍以下で、後半 5 回の最大が前半 5〜10 回の最大の 1.1 倍以下
        withClue("GC 後のヒープ使用量（バイト）: $used") {
            used[19] shouldBeLessThanOrEqual (used[4] * 1.1).toLong()
            used.subList(15, 20).max() shouldBeLessThanOrEqual (used.subList(5, 10).max() * 1.1).toLong()
        }
    }

    case("MEM-03", "ピークヒープが KSP2 以下（同じ Processor・同じ入力）", pending = PendingCase(Milestone.M2)) {
        // 同じ TP（KSP 版）を K と B で動かす。毎回新しい daemon で、ピークを戻してから生成だけを流す
        val dir = generateLarge(files = 1000)

        fun peak(engine: SampleEngine): Long {
            stopDaemons(dir)
            benchRunner(dir, engine, "clean").build()
            return List(3) { benchRunner(dir, engine, "clean", "acgHeapReset", "acgCodegen", "acgHeap").build().heap().second }.min()
        }
        val k = peak(SampleEngine.Ksp2)
        val b = peak(SampleEngine.Bridge)
        withClue("ピークヒープ K=$k B=$b（バイト）") { b shouldBeLessThanOrEqual k }
    }
})

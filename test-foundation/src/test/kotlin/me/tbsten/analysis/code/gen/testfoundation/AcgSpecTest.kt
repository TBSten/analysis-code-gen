@file:OptIn(io.kotest.common.KotestInternal::class)

package me.tbsten.analysis.code.gen.testfoundation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.Spec
import io.kotest.core.spec.SpecRef
import io.kotest.core.spec.style.FreeSpec
import io.kotest.engine.TestEngineLauncher
import io.kotest.engine.listener.CollectingTestEngineListener
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class AcgSpecTest : FreeSpec({
    fun newSample(): AcgSpec = sampleE2ESpecClass.getDeclaredConstructor().also { it.isAccessible = true }.newInstance() as AcgSpec

    @Suppress("UNCHECKED_CAST")
    suspend fun runSample(specClass: Class<*> = sampleE2ESpecClass): CollectingTestEngineListener {
        val listener = CollectingTestEngineListener()
        TestEngineLauncher()
            .withListener(listener)
            .withSpecRefs(SpecRef.Reference((specClass as Class<out Spec>).kotlin))
            .execute()
        return listener
    }

    afterEach {
        CaseRuntime.runPendingOverride = null
        CaseRuntime.pendingResultsDirOverride = null
        CaseRuntime.pendingTimeoutOverride = null
        CaseRuntime.pendingShardOverride = null
        SampleCounter.reset()
    }

    "case の宣言が ID・種別・保留つきで集まる" {
        val decls = newSample().caseDeclarations
        decls.map { it.id } shouldContainExactly listOf("GRD-01", "GRD-02", "GRD-03")
        decls.map { it.kind }.toSet() shouldBe setOf(CaseKind.E2E)
        decls[0].pending shouldBe null
        decls[1].pending shouldBe PendingCase(Milestone.M1, PendingReason.NotImplemented)
        decls[2].pending?.reason?.code shouldBe "ADR-031"
        decls[0].testName shouldBe "[GRD-01] 有効なケース"
        decls[0].specClass shouldBe "me.tbsten.analysis.code.gen.testfoundation.SampleE2ESpec"
    }

    "書式違いの ID は spec の生成時に失敗する" {
        val e =
            shouldThrow<InvocationTargetException> {
                sampleBrokenSpecClass.getDeclaredConstructor().also { it.isAccessible = true }.newInstance()
            }
        e.cause!!.message shouldContain "grd-1"
    }

    "通常の実行では保留ケースは skip され、有効なケースと補助テストだけが走る" {
        CaseRuntime.runPendingOverride = false
        val listener = runSample()
        SampleCounter.enabledRuns shouldBe 1
        SampleCounter.helperRuns shouldBe 1
        SampleCounter.pendingRuns shouldBe 0
        listener.errors shouldBe false
        listener.tests
            .filterValues { it.isIgnored }
            .keys
            .map { it.name.name } shouldContainExactlyInAnyOrder
            listOf("[GRD-02] 保留で中身は失敗するケース", "[GRD-03] 保留だが中身は通るケース")
        listener.tests
            .filterValues { it.isSuccess }
            .keys
            .map { it.name.name } shouldContainExactlyInAnyOrder
            listOf("[GRD-01] 有効なケース", "ID なしの補助テスト")
    }

    "runPending では保留ケースも中身を実行し、失敗しても落とさず結果を書く" {
        val dir = Files.createTempDirectory("acg-pending")
        CaseRuntime.runPendingOverride = true
        CaseRuntime.pendingResultsDirOverride = dir
        val listener = runSample()
        SampleCounter.pendingRuns shouldBe 2
        listener.errors shouldBe false
        listener.tests.values.all { it.isSuccess } shouldBe true

        val runs = dir.resolve("pending-runs").listDirectoryEntries().map { it.readText().trim().split("\t") }
        runs.associate { it[0] to it[1] } shouldBe mapOf("GRD-02" to "failed", "GRD-03" to "passed")
        val unexpected = dir.resolve("unexpected-pass").listDirectoryEntries().map { it.readText().trim() }
        unexpected shouldContainExactly listOf("GRD-03\t[GRD-03] 保留だが中身は通るケース")
    }

    "runPending で持ち時間を超えた保留ケースは timeout と記録して次へ進む（ADR-057）" {
        val dir = Files.createTempDirectory("acg-pending-timeout")
        CaseRuntime.runPendingOverride = true
        CaseRuntime.pendingResultsDirOverride = dir
        CaseRuntime.pendingTimeoutOverride = 200.milliseconds
        val started = System.nanoTime()
        val listener = runSample(sampleHangingSpecClass)
        (System.nanoTime() - started).nanoseconds shouldBeLessThan 30.seconds
        listener.errors shouldBe false
        listener.tests.values.all { it.isSuccess } shouldBe true
        val run =
            dir
                .resolve("pending-runs")
                .listDirectoryEntries()
                .single()
                .readText()
                .trim()
                .split("\t")
        run[0] shouldBe "GRD-04"
        run[1] shouldBe "timeout"
        run[4] shouldContain "PendingTimeoutException"
        dir.resolve("unexpected-pass").toFile().exists() shouldBe false
    }

    "pendingShard を付けると、その分割に入る保留ケースだけを実行し、有効なケースは実行しない（ADR-057）" {
        val dir = Files.createTempDirectory("acg-pending-shard")
        CaseRuntime.runPendingOverride = true
        CaseRuntime.pendingResultsDirOverride = dir
        val shard = (0 until 4).first { i -> CaseRuntime.inShard("GRD-02", i to 4) }
        CaseRuntime.pendingShardOverride = shard to 4
        runSample()
        SampleCounter.enabledRuns shouldBe 0
        val ran = dir.resolve("pending-runs").listDirectoryEntries().map { it.readText().trim().split("\t")[0] }
        ran shouldContain "GRD-02"
        ran.forEach { CaseRuntime.inShard(it, shard to 4) shouldBe true }
    }

    "分割の指定は i/n で、各 ID はちょうど 1 つの分割に入る" {
        CaseRuntime.parseShard("2/4") shouldBe (2 to 4)
        shouldThrow<IllegalArgumentException> { CaseRuntime.parseShard("4/4") }
        shouldThrow<IllegalArgumentException> { CaseRuntime.parseShard("x") }
        listOf("GRD-01", "INC-14a", "KSPT-Hello").forEach { id ->
            (0 until 3).count { CaseRuntime.inShard(id, it to 3) } shouldBe 1
        }
    }
})

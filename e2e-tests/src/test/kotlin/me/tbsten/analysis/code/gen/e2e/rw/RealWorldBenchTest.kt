package me.tbsten.analysis.code.gen.e2e.rw

import me.tbsten.analysis.code.gen.e2e.support.RealWorldSample
import me.tbsten.analysis.code.gen.e2e.support.SampleEngine
import me.tbsten.analysis.code.gen.e2e.support.measureFullCodegen
import me.tbsten.analysis.code.gen.e2e.support.shouldNotBeSlowerThan
import me.tbsten.analysis.code.gen.testfoundation.BenchSpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase

// RW-ALL-P1: 各 Processor の Bridge 版で、サンプルの全件処理の時間が KSP2 以下。計測の決まりは PERF と同じ（ADR-037）

private fun bridgeShouldNotBeSlowerThanKsp2(
    processorId: String,
    sample: String,
) {
    val dir = RealWorldSample.open(processorId, sample).projectDir
    val k = measureFullCodegen(dir, SampleEngine.Ksp2, "$processorId/$sample/K")
    val b = measureFullCodegen(dir, SampleEngine.Bridge, "$processorId/$sample/B")
    shouldNotBeSlowerThan("RW-ALL-P1", b, k)
}

class RealWorldBenchTest : BenchSpec({
    case(
        "RW-ALL-P1",
        "cream.kt・Moshi・Koin・kotlin-inject の Bridge 版で全件処理の時間が KSP2 以下",
        pending = PendingCase(Milestone.M2),
    ) {
        listOf("CRM", "MSH", "KOI", "KIN").forEach { bridgeShouldNotBeSlowerThanKsp2(it, "jvm") }
    }

    // Room / Dagger は M3。同じケース ID の 2 本目で、マイルストーンは一覧の M 列の最初の値（M2）に合わせる（ADR-030）
    case(
        "RW-ALL-P1",
        "Room・Dagger・Hilt の Bridge 版で全件処理の時間が KSP2 以下",
        pending = PendingCase(Milestone.M2),
    ) {
        bridgeShouldNotBeSlowerThanKsp2("ROM", "android")
        bridgeShouldNotBeSlowerThanKsp2("DAG", "jvm")
        bridgeShouldNotBeSlowerThanKsp2("DAG", "hilt-android")
    }
})

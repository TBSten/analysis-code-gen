package me.tbsten.analysiscodegen.testfoundation

import io.kotest.core.annotation.Ignored

// テスト用の spec。private にして kotest の自動実行からは外し、TestEngineLauncher や CaseCollector から直接使う

private class SampleE2ESpec : E2ESpec({
    case("GRD-01", "有効なケース") {
        SampleCounter.enabledRuns++
    }
    case("GRD-02", "保留で中身は失敗するケース", pending = PendingCase(Milestone.M1)) {
        SampleCounter.pendingRuns++
        error("未実装")
    }
    case("GRD-03", "保留だが中身は通るケース", pending = PendingCase(Milestone.M1, PendingReason.Adr(31))) {
        SampleCounter.pendingRuns++
    }
    "ID なしの補助テスト" {
        SampleCounter.helperRuns++
    }
})

object SampleCounter {
    var enabledRuns = 0
    var pendingRuns = 0
    var helperRuns = 0

    fun reset() {
        enabledRuns = 0
        pendingRuns = 0
        helperRuns = 0
    }
}

internal val sampleE2ESpecClass: Class<*> = Class.forName("me.tbsten.analysiscodegen.testfoundation.SampleE2ESpec")

private class SampleUnitSpec : UnitSpec({
    case("INC-01a", "細分化した ID も使える") {}
})

internal val sampleUnitSpecClass: Class<*> = Class.forName("me.tbsten.analysiscodegen.testfoundation.SampleUnitSpec")

private abstract class SampleAbstractSpec : PbtSpec()

internal val sampleAbstractSpecClass: Class<*> = Class.forName("me.tbsten.analysiscodegen.testfoundation.SampleAbstractSpec")

@Ignored
private class SampleIgnoredSpec : E2ESpec({
    case("GRD-04", "@Ignored で隠したケース") {}
})

internal val sampleIgnoredSpecClass: Class<*> = Class.forName("me.tbsten.analysiscodegen.testfoundation.SampleIgnoredSpec")

private class SampleBrokenSpec : E2ESpec({
    case("grd-1", "書式違いの ID") {}
})

internal val sampleBrokenSpecClass: Class<*> = Class.forName("me.tbsten.analysiscodegen.testfoundation.SampleBrokenSpec")

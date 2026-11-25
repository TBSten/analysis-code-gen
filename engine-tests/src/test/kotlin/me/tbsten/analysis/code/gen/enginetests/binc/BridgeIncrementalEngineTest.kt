@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.enginetests.binc

import io.kotest.matchers.types.shouldBeInstanceOf
import me.tbsten.analysis.code.gen.incremental.DirtyFileCalculator
import me.tbsten.analysis.code.gen.incremental.IncrementalPlan
import me.tbsten.analysis.code.gen.incremental.IncrementalState
import me.tbsten.analysis.code.gen.incremental.InputSnapshot
import me.tbsten.analysis.code.gen.incremental.OutputDependencyKind
import me.tbsten.analysis.code.gen.incremental.OutputRecord
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import me.tbsten.analysis.code.gen.testfoundation.IntegrationSpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase

// 5.15 Bridge の増分互換（BINC）のうち I。Bridge の Lookup 追跡は IncrementalParticipant として dirty 計算に加わり、
// 追跡できない操作があったら null（= 全件処理）を返す（ADR-017）

private data class State(
    override val snapshot: InputSnapshot,
    override val outputs: List<OutputRecord>,
) : IncrementalState

class BridgeIncrementalEngineTest : IntegrationSpec({
    case("BINC-05", "追跡できない操作があったら、保守的に全件処理する", pending = PendingCase(Milestone.M3)) {
        val sources = mapOf("A.kt" to "1", "B.kt" to "1", "C.kt" to "1")
        val previous =
            State(
                InputSnapshot(sources, "g", emptyMap()),
                listOf(OutputRecord("kotlin/AGen.kt", "ksp:test", OutputDependencyKind.Isolating, setOf("A.kt"), emptySet())),
            )
        val current = InputSnapshot(sources + ("B.kt" to "2"), "g", emptyMap())
        // Bridge の参加口が「追跡できない」（null）と答えたら、isolating の契約では B.kt だけで済む変更でも全件処理
        DirtyFileCalculator().plan(previous, current) { _, _ -> null }.shouldBeInstanceOf<IncrementalPlan.Full>()
        // 追跡できたとき（空集合）は増分のまま
        DirtyFileCalculator().plan(previous, current) { _, _ -> emptySet() }.shouldBeInstanceOf<IncrementalPlan.Incremental>()
    }
})

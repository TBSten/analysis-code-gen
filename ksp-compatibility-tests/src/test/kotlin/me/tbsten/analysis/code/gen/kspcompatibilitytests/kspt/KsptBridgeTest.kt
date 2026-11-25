package me.tbsten.analysis.code.gen.kspcompatibilitytests.kspt

import me.tbsten.analysis.code.gen.testfoundation.DiffSpec
import me.tbsten.analysis.code.gen.testfoundation.PendingCase

/**
 * KSPT-*: KSP 公式テストを Bridge で流し、KSP2 と同じ出力になる（ADR-024、BRG-07）。
 *
 * 保留を外すときは `kspt/bridge-enabled.txt` に ID を足す（期待値は上流の testData のまま変えない）。
 */
class KsptBridgeTest :
    DiffSpec({
        for (c in KsptCases.all) {
            val pending = if (c.id in KsptCases.bridgeEnabled) null else PendingCase(c.milestone)
            case(c.id, c.title, pending = pending) {
                KsptCases.assertPasses(c, KsptEngine.Bridge)
            }
        }
    })

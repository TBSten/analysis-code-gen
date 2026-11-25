package fixture.jvm

import me.tbsten.analysis.code.gen.tp.Chain
import me.tbsten.analysis.code.gen.tp.DeferUntil

/** TP-Chain が PipelineChain1 → 2 → 3 をラウンドごとに作る。 */
@Chain(depth = 3)
class Pipeline

/** PipelineChain3（ラウンド 3 の生成物）が見えるまで TP-Defer が持ち越す。 */
@DeferUntil("fixture.jvm.PipelineChain3")
class WaitsForChain

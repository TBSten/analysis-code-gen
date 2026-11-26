@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.incremental

import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import me.tbsten.analysis.code.gen.processor.OptionImpact

/**
 * Processor のオプションが変わったとき、全件か増分かを決める（ADR-051。ADR-015 の「オプションが変わったら全件」を上書き）。
 */
@InternalAnalysisCodegenApi
public object OptionChangeJudge {
    /**
     * 前回と今回のオプションをキー単位で比べ、変わったキーの影響のうち最も大きいものを返す。
     * 変わったキーが無ければ [OptionImpact.None]。値の追加・削除も変化として数える。
     *
     * [impactOf] は Processor の宣言。宣言の無いキーと KSP Bridge 経由の Processor は `{ OptionImpact.Full }` を渡す。
     */
    public fun judge(
        previous: Map<String, String>,
        current: Map<String, String>,
        impactOf: (key: String) -> OptionImpact,
    ): OptionImpact {
        val changedKeys = (previous.keys + current.keys).filter { previous[it] != current[it] }
        val impacts = changedKeys.map(impactOf)
        return when {
            OptionImpact.Full in impacts -> OptionImpact.Full
            OptionImpact.Incremental in impacts -> OptionImpact.Incremental
            else -> OptionImpact.None
        }
    }
}

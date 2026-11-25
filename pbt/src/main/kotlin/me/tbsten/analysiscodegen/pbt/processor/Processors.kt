package me.tbsten.analysiscodegen.pbt.processor

import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.exhaustive.exhaustive

/**
 * 対象 Processor（ADR-019 の `fixtures/processors/processors.toml` と同じ ID）。
 * @property kmp commonMain などで使えるか
 * @property native Native 移植版を作るか（REQUIRED は合格ラインに入る）
 */
public enum class ProcessorId(
    public val catalogName: String,
    public val kmp: Boolean,
    public val native: NativePort,
) {
    CRM("cream", kmp = true, native = NativePort.REQUIRED),
    MSH("moshi", kmp = false, native = NativePort.REQUIRED),
    KOI("koin", kmp = true, native = NativePort.REQUIRED),
    KIN("kotlin-inject", kmp = true, native = NativePort.OPTIONAL),
    DAG("dagger", kmp = false, native = NativePort.OPTIONAL),
    ROM("room", kmp = true, native = NativePort.NONE),
    ;

    public companion object {
        public fun fromCatalogName(name: String): ProcessorId? =
            entries.firstOrNull { it.catalogName == name || it.name.equals(name, ignoreCase = true) }
    }
}

public enum class NativePort { REQUIRED, OPTIONAL, NONE }

/** Processor をどこに適用するか（pbt-differential-design.md 2.1）。 */
public enum class Placement {
    SINGLE_MODULE,
    MULTI_MODULE,
    KMP_COMMON,
    KMP_PLATFORM,
    ;

    public val isKmp: Boolean get() = this == KMP_COMMON || this == KMP_PLATFORM
}

/** Processor の組。[order] は登録順（結果が順番に依存しないことも確かめるため、順番も値に含める）。 */
public data class ProcessorSet(
    val order: List<ProcessorId>,
    val placement: Placement = Placement.SINGLE_MODULE,
) {
    init {
        require(order.isNotEmpty()) { "Processor の組が空" }
        require(order.distinct().size == order.size) { "同じ Processor が 2 回入っている: $order" }
        require(!placement.isKmp || order.all { it.kmp }) { "KMP に置けない Processor がある: $order @ $placement" }
    }

    val members: Set<ProcessorId> get() = order.toSet()

    override fun toString(): String = order.joinToString("+") + "@" + placement.name.lowercase()

    public companion object {
        /** 必ず含める現実的な組み合わせ（設計 2.1 節） */
        public val REALISTIC: List<ProcessorSet> =
            listOf(
                ProcessorSet(listOf(ProcessorId.KOI, ProcessorId.CRM)),
                ProcessorSet(listOf(ProcessorId.MSH, ProcessorId.KIN)),
                ProcessorSet(listOf(ProcessorId.ROM, ProcessorId.MSH)),
                ProcessorSet(listOf(ProcessorId.DAG, ProcessorId.ROM, ProcessorId.MSH)),
                ProcessorSet(listOf(ProcessorId.KOI, ProcessorId.MSH, ProcessorId.CRM)),
            )

        /** 決定的な網羅層: 単体の全部と、2 個の全ペア（順番の入れ替えを含む）。単一モジュールに置く */
        public fun singlesAndOrderedPairs(): List<ProcessorSet> {
            val ids = ProcessorId.entries
            return ids.map { ProcessorSet(listOf(it)) } +
                ids.flatMap { a -> ids.filter { it != a }.map { b -> ProcessorSet(listOf(a, b)) } }
        }

        /** 置き場所に置ける組か */
        public fun allowed(
            order: List<ProcessorId>,
            placement: Placement,
        ): Boolean = order.isNotEmpty() && order.distinct().size == order.size && (!placement.isKmp || order.all { it.kmp })
    }
}

/** Processor の組の Arb。空でない部分集合 × 並べ替え × 置き場所。edgecase は現実的な組み合わせ。 */
public fun Arb.Companion.processorSet(max: Int = ProcessorId.entries.size): Arb<ProcessorSet> =
    arbitrary(edgecases = ProcessorSet.REALISTIC, shrinker = ProcessorSetShrinker) { rs ->
        val r = rs.random
        val placement = Placement.entries[r.nextInt(Placement.entries.size)]
        val pool = ProcessorId.entries.filter { !placement.isKmp || it.kmp }.shuffled(r)
        val n = 1 + r.nextInt(minOf(max, pool.size))
        ProcessorSet(pool.take(n), placement)
    }

/** 決定的な網羅層の Exhaustive（単体 + 順序付きペア = 36 通り）。 */
public fun Exhaustive.Companion.processorSets(): Exhaustive<ProcessorSet> = ProcessorSet.singlesAndOrderedPairs().exhaustive()

public object ProcessorSetShrinker : io.kotest.property.Shrinker<ProcessorSet> {
    override fun shrink(value: ProcessorSet): List<ProcessorSet> {
        val out = mutableListOf<ProcessorSet>()
        if (value.placement != Placement.SINGLE_MODULE) out += value.copy(placement = Placement.SINGLE_MODULE)
        if (value.order.size > 1) {
            value.order.indices.forEach { i -> out += value.copy(order = value.order.filterIndexed { j, _ -> j != i }) }
            val sorted = value.order.sortedBy { it.ordinal }
            if (sorted != value.order) out += value.copy(order = sorted)
        }
        return out.filter { ProcessorSet.allowed(it.order, it.placement) }.distinct()
    }
}

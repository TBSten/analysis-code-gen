package me.tbsten.analysiscodegen.pbt.processor

import io.kotest.property.Arb
import io.kotest.property.Shrinker
import io.kotest.property.arbitrary.arbitrary

/**
 * Processor のオプション 1 つ。[values] の先頭が既定値（縮小はここへ寄せる）。
 * 真偽値・列挙は全値、文字列・数値は代表値と境界値を並べる（設計 2.4 節）。
 */
public data class OptionSpec(
    val processor: ProcessorId,
    val name: String,
    val values: List<String>,
)

/** Processor ごとのオプション表。リソース `pbt/options.tsv`（列: Processor・名前・値をカンマ区切り）から読む。 */
public object OptionTable {
    public val all: List<OptionSpec> by lazy { parse(OptionTable::class.java.getResource("/pbt/options.tsv")!!.readText()) }

    public fun of(p: ProcessorId): List<OptionSpec> = all.filter { it.processor == p }

    public fun parse(text: String): List<OptionSpec> =
        text
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { line ->
                val c = line.split('\t')
                require(c.size >= 3) { "options.tsv の行の形が違う: $line" }
                OptionSpec(ProcessorId.valueOf(c[0]), c[1], c[2].split(',').map { it.trim() })
            }.toList()
}

/** Processor の組に対するオプションの割り当て（オプション名 → 値）。既定値のものは入れない。 */
public data class OptionAssignment(
    val values: Map<String, String>,
) {
    override fun toString(): String = values.entries.joinToString(",") { "${it.key}=${it.value}" }.ifEmpty { "(defaults)" }
}

/** オプションの Arb。各オプションを 50% で既定値以外にする。縮小は既定値へ戻す方向。 */
public fun Arb.Companion.options(set: ProcessorSet): Arb<OptionAssignment> {
    val specs = set.order.flatMap { OptionTable.of(it) }
    return arbitrary(edgecases = listOf(OptionAssignment(emptyMap())), shrinker = OptionShrinker(specs)) { rs ->
        val r = rs.random
        OptionAssignment(
            specs
                .mapNotNull { s ->
                    if (s.values.size < 2 || r.nextBoolean()) null else s.name to s.values[1 + r.nextInt(s.values.size - 1)]
                }.toMap(),
        )
    }
}

public class OptionShrinker(
    private val specs: List<OptionSpec>,
) : Shrinker<OptionAssignment> {
    override fun shrink(value: OptionAssignment): List<OptionAssignment> =
        value.values.keys.map { k -> OptionAssignment(value.values - k) } +
            value.values.mapNotNull { (k, v) ->
                val s = specs.firstOrNull { it.name == k } ?: return@mapNotNull null
                val i = s.values.indexOf(v)
                if (i > 1) OptionAssignment(value.values + (k to s.values[i - 1])) else null
            }
}

/**
 * 全ペア網羅（pairwise の covering array）を貪欲法で作る（決定的な網羅層、XD-07）。
 * 返す各行は各パラメータの値の添字。どの 2 パラメータの値の組も、少なくとも 1 行に現れる。
 * 同じ入力からは常に同じ行を返す（乱数を使わない）。
 */
public object Pairwise {
    public fun cover(sizes: List<Int>): List<IntArray> {
        require(sizes.all { it >= 1 }) { "値の無いパラメータがある: $sizes" }
        val n = sizes.size
        if (n == 0) return listOf(IntArray(0))
        if (n == 1) return (0 until sizes[0]).map { intArrayOf(it) }
        // まだ覆っていない組: (i, a, j, b) で i < j
        val uncovered = HashSet<Long>()
        for (i in 0 until n) for (j in i + 1 until n) for (a in 0 until sizes[i]) for (b in 0 until sizes[j]) uncovered += key(i, a, j, b)
        val rows = mutableListOf<IntArray>()
        while (uncovered.isNotEmpty()) {
            // 覆っていない組を 1 つ選んで種にし、残りのパラメータを 1 つずつ「新しく覆う組が最も多い値」で埋める
            val seed = uncovered.min()
            val (si, sa, sj, sb) = unkey(seed)
            val row = IntArray(n) { -1 }
            row[si] = sa
            row[sj] = sb
            for (k in 0 until n) {
                if (row[k] >= 0) continue
                var best = 0
                var bestGain = -1
                for (v in 0 until sizes[k]) {
                    var gain = 0
                    for (m in 0 until n) {
                        if (m == k || row[m] < 0) continue
                        val kk = if (m < k) key(m, row[m], k, v) else key(k, v, m, row[m])
                        if (kk in uncovered) gain++
                    }
                    if (gain > bestGain) {
                        bestGain = gain
                        best = v
                    }
                }
                row[k] = best
            }
            for (i in 0 until n) for (j in i + 1 until n) uncovered -= key(i, row[i], j, row[j])
            rows += row
        }
        return rows
    }

    private fun key(
        i: Int,
        a: Int,
        j: Int,
        b: Int,
    ): Long = (i.toLong() shl 48) or (a.toLong() shl 32) or (j.toLong() shl 16) or b.toLong()

    private fun unkey(k: Long): IntArray =
        intArrayOf((k ushr 48).toInt() and 0xFFFF, (k ushr 32).toInt() and 0xFFFF, (k ushr 16).toInt() and 0xFFFF, k.toInt() and 0xFFFF)

    /** Processor の組に対するオプションの全ペア網羅。 */
    public fun options(set: ProcessorSet): List<OptionAssignment> {
        val specs = set.order.flatMap { OptionTable.of(it) }
        if (specs.isEmpty()) return listOf(OptionAssignment(emptyMap()))
        return cover(specs.map { it.values.size }).map { row ->
            OptionAssignment(specs.mapIndexedNotNull { i, s -> if (row[i] == 0) null else s.name to s.values[row[i]] }.toMap())
        }
    }
}

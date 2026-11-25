package me.tbsten.analysis.code.gen.pbt.catalog

import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Shrinker
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.exhaustive.exhaustive
import me.tbsten.analysis.code.gen.pbt.processor.ProcessorSet
import me.tbsten.analysis.code.gen.pbt.run.Shards

/**
 * カタログのパターンを組み合わせた 1 つの入力プロジェクト（`docs/design/input-pattern-catalog.md` の組み立て規則）。
 * 中身のソースは [PatternCatalog.sources] で読む（モデルは ID の組だけを持ち、縮小もパターン単位で行う）。
 */
public data class UserProject(
    /** 主のパターン（Processor の組に合うもの）。1〜3 個 */
    val primary: List<CatalogPattern>,
    /** 背景コード（`PROCESSORS: *`）。0〜2 個 */
    val background: List<CatalogPattern> = emptyList(),
) {
    val patterns: List<CatalogPattern> get() = primary + background
    val ids: List<String> get() = patterns.map { it.id }

    /** 全体の期待結果。エラーのパターンは 1 つまでなので、あればそれ */
    val expect: Expect get() =
        patterns.firstOrNull { it.isError }?.expect
            ?: if (patterns.any { it.expect == Expect.WARNING }) Expect.WARNING else Expect.OK

    /** 使うオプションの組（パターンが持っていれば最初のもの）。無ければ Arb<Options> に任せる */
    val preferredOptions: Map<String, String>? get() = patterns.firstNotNullOfOrNull { it.options.firstOrNull() }

    override fun toString(): String = "UserProject(${ids.joinToString(" + ")})"

    public companion object {
        public fun of(p: CatalogPattern): UserProject = if (p.background) UserProject(emptyList(), listOf(p)) else UserProject(listOf(p))

        /**
         * 組み立て規則を守って合成する。守れないもの（エラーの 2 個目・同じファイルパス）は落とす。
         * パッケージの付け替えはまだしない（ACG-42 で K に通して要るかを確かめる）。代わりに FILE のパスがぶつかる組を避ける。
         */
        public fun compose(
            primary: List<CatalogPattern>,
            background: List<CatalogPattern>,
        ): UserProject {
            val files = HashSet<String>()
            var hasError = false
            val ids = HashSet<String>()

            fun accept(p: CatalogPattern): Boolean {
                if (!ids.add(p.id)) return false
                if (p.isError && hasError) return false
                if (p.files.any { it in files }) return false
                files += p.files
                hasError = hasError || p.isError
                return true
            }
            val pr = primary.filter(::accept)
            val bg = background.filter(::accept)
            return UserProject(pr, bg)
        }
    }
}

/** パターンが Processor の組に合うか（PROCESSORS が組に含まれる。背景コードはどの組にも合う）。 */
public fun CatalogPattern.fits(set: ProcessorSet): Boolean = background || (processors.isNotEmpty() && set.members.containsAll(processors))

/**
 * Processor の組に対する入力プロジェクトの Arb（ランダム層）。
 *
 * - 主のパターン 1〜3 個 + 背景コード 0〜2 個
 * - edgecase は「合うパターン 1 個だけの入力」を全部（= カタログの各パターンが必ず 1 回は出る候補になる。XD-01）
 * - 縮小はパターン単位で外す（主は 1 個まで、背景は 0 個まで）
 *
 * @param pool 使うパターン。既定は verified だけ（ADR-020 の 6 項）。candidate を K に通す網羅層は [Exhaustive.Companion.catalogShard]
 */
public fun Arb.Companion.userProject(
    catalog: PatternCatalog,
    set: ProcessorSet,
    pool: List<CatalogPattern> = catalog.verified,
): Arb<UserProject> {
    val fitting = pool.filter { !it.background && it.fits(set) }
    val backgrounds = pool.filter { it.background }
    require(fitting.isNotEmpty()) { "Processor の組 $set に合うパターンが無い（pool ${pool.size} 件）" }
    return arbitrary(edgecases = userProjectEdgecases(set, pool), shrinker = UserProjectShrinker) { rs ->
        val r = rs.random
        val primary = List(1 + r.nextInt(3)) { fitting[r.nextInt(fitting.size)] }
        val bg = if (backgrounds.isEmpty()) emptyList() else List(r.nextInt(3)) { backgrounds[r.nextInt(backgrounds.size)] }
        UserProject.compose(primary, bg)
    }
}

/** [userProject] の edgecase（合うパターン 1 個だけの入力）。 */
public fun userProjectEdgecases(
    set: ProcessorSet,
    pool: List<CatalogPattern>,
): List<UserProject> = pool.filter { !it.background && it.fits(set) }.map { UserProject.of(it) }

public object UserProjectShrinker : Shrinker<UserProject> {
    override fun shrink(value: UserProject): List<UserProject> {
        val out = mutableListOf<UserProject>()
        value.background.indices.forEach { i -> out += value.copy(background = value.background.filterIndexed { j, _ -> j != i }) }
        if (value.primary.size >
            1
        ) {
            value.primary.indices.forEach { i -> out += value.copy(primary = value.primary.filterIndexed { j, _ -> j != i }) }
        }
        return out
    }
}

/**
 * 決定的な網羅層: 区画 [shard] のパターン（rejected 以外）を 1 パターン 1 入力で並べる。
 * [me.tbsten.analysis.code.gen.pbt.run.acgCheckExhaustive] に渡す（kotest の Exhaustive は空を作れないため一覧で返す）。
 */
public fun PatternCatalog.shard(shard: Int): List<UserProject> =
    usable.filter { it.shardOrComputed(shards) == shard }.map { UserProject.of(it) }

/** 区画が空でなければ kotest の Exhaustive にする。 */
public fun Exhaustive.Companion.catalogShard(
    catalog: PatternCatalog,
    shard: Int,
): Exhaustive<UserProject>? = catalog.shard(shard).takeIf { it.isNotEmpty() }?.exhaustive()

/** index.json の shard（無ければ計算）。 */
public fun CatalogPattern.shardOrComputed(shards: Int = 24): Int = if (shard >= 0) shard else Shards.of(id, shards)

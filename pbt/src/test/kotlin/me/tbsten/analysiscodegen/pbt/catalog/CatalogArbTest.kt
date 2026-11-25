package me.tbsten.analysiscodegen.pbt.catalog

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import me.tbsten.analysiscodegen.pbt.processor.ProcessorId
import me.tbsten.analysiscodegen.pbt.processor.ProcessorSet
import me.tbsten.analysiscodegen.pbt.run.Shards
import me.tbsten.analysiscodegen.testfoundation.UnitSpec
import java.io.File

/** リポジトリのカタログ（ACG-41）があればそれを、無ければテスト用の小さなカタログを使う。 */
private fun catalogs(): List<PatternCatalog> {
    val fixture = File(CatalogArbTest::class.java.getResource("/catalog-fixture/index.json")!!.toURI())
    return listOfNotNull(PatternCatalog.loadOrNull(fixture), PatternCatalog.loadOrNull())
}

private fun setFor(p: CatalogPattern): ProcessorSet = ProcessorSet(p.processors.sortedBy { it.ordinal })

class CatalogArbTest : UnitSpec({
    case("XD-01", "入力パターンカタログの全パターンが Arb から生成されうる（カタログと Arb がずれていない）") {
        for (catalog in catalogs()) {
            withClue("カタログ ${catalog.baseDir}（${catalog.patterns.size} 件）") {
                val usable = catalog.usable

                // 1. 決定的な網羅層: 24 区画を合わせると、使えるパターンがちょうど 1 回ずつ出る
                val fromShards = (0 until catalog.shards).flatMap { s -> catalog.shard(s).flatMap { it.ids } }
                fromShards shouldContainExactlyInAnyOrder usable.map { it.id }

                // 2. index.json の区画は ID の CRC32 % 24 と一致する（Kotlin 側と catalog.py 側の式がずれていない）
                usable
                    .filter { it.shard >= 0 }
                    .filter { it.shard != Shards.of(it.id, catalog.shards) }
                    .map { it.id }
                    .shouldBeEmpty()

                // 3. ランダム層: 背景でないパターンは、自分の Processor の組の Arb の edgecase に必ず入る
                val unknown = usable.filter { !it.background && it.processors.isEmpty() }
                withClue("Processor が分からないパターン") { unknown.map { it.id }.shouldBeEmpty() }
                for (p in usable.filter { !it.background }) {
                    userProjectEdgecases(setFor(p), usable).map { it.ids }.contains(listOf(p.id)) shouldBe true
                }

                // 4. 背景コードは、どの組の Arb でも選ばれうる（十分な回数引くと全部出る）
                val bgs = usable.filter { it.background }.map { it.id }.toSet()
                val anySet = setFor(usable.first { !it.background })
                val arb = Arb.userProject(catalog, anySet, usable)
                val rs = RandomSource.seeded(1L)
                val drawn = HashSet<String>()
                repeat(200 * (bgs.size + 1)) {
                    val up = arb.sample(rs).value
                    drawn += up.background.map { it.id }
                    // 組み立て規則: 主は組に合う・エラーは 1 個まで・同じファイルパスは無い
                    up.primary.all { it.fits(anySet) } shouldBe true
                    up.patterns.count { it.isError } shouldBeLessThanOrEqual 1
                    up.patterns.flatMap { it.files }.let { it.size shouldBe it.toSet().size }
                }
                drawn shouldBe bgs

                // 5. index.json の files と、パターン本体の FILE 区切りが一致する
                for (p in usable) {
                    withClue(p.id) { catalog.sources(p).keys.toList() shouldBe p.files }
                }
            }
        }
    }

    "rejected は使わず、ランダム層の既定は verified だけ" {
        val catalog = catalogs().first()
        catalog.usable.none { it.status == PatternStatus.REJECTED } shouldBe true
        catalog.verified.all { it.status == PatternStatus.VERIFIED } shouldBe true
        val arb = Arb.userProject(catalog, ProcessorSet(listOf(ProcessorId.KOI, ProcessorId.CRM)))
        val rs = RandomSource.seeded(2L)
        repeat(100) {
            arb
                .sample(rs)
                .value.patterns
                .all { it.status == PatternStatus.VERIFIED } shouldBe true
        }
    }

    "options はオブジェクトの配列として読む" {
        val catalog = catalogs().first()
        catalog.patterns.first { it.id == "KOI-O-0001" }.options shouldBe listOf(mapOf("KOIN_CONFIG_CHECK" to "true"))
    }

    "縮小はパターン単位で、主は 1 個まで残す" {
        val catalog = catalogs().first()
        val all = catalog.usable
        val up = UserProject(all.filter { !it.background }.take(2), all.filter { it.background })
        UserProjectShrinker.shrink(up).all { it.primary.isNotEmpty() } shouldBe true
    }
})

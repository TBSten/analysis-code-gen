package me.tbsten.analysis.code.gen.e2e.xd

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import me.tbsten.analysis.code.gen.differential.DiagnosticRecord
import me.tbsten.analysis.code.gen.differential.EngineRun
import me.tbsten.analysis.code.gen.differential.FileKResultStore
import me.tbsten.analysis.code.gen.differential.KspReferenceRunner
import me.tbsten.analysis.code.gen.differential.RunOutput
import me.tbsten.analysis.code.gen.e2e.support.catalog
import me.tbsten.analysis.code.gen.e2e.support.toDiffInput
import me.tbsten.analysis.code.gen.pbt.catalog.CatalogPattern
import me.tbsten.analysis.code.gen.pbt.catalog.Expect
import me.tbsten.analysis.code.gen.pbt.processor.ProcessorId
import me.tbsten.analysis.code.gen.pbt.processor.ProcessorSet
import me.tbsten.analysis.code.gen.pbt.run.Shards
import java.io.File
import kotlin.system.exitProcess

// ACG-68（ADR-072）: 入力パターンカタログの各パターンを K（KSP2）で動かし、EXPECT どおりかで verified / rejected に振り分ける道具。
// ケースのテストではない（進捗に数えない）。`./gradlew :e2e-tests:verifyCatalog` から動かし、結果の TSV を
// `catalog.py apply-k` が各パターンの STATUS と NOTE に反映する。
//
// 判定は XD-02 のアサーションと同じにする（verified = そのパターン 1 個だけの入力で XD-02 が通る）。
// 入力の作り方も XD-02 と同じ toDiffInput を使うので、verified にしたパターンは XD-02 の edgecase でそのまま通る。

/** 背景コード（`PROCESSORS: *`）はどの組にも混ぜるので、KSP 版のある主な Processor それぞれの単体で確かめる。 */
private val BACKGROUND_SETS = listOf(ProcessorId.CRM, ProcessorId.MSH, ProcessorId.KOI).map { ProcessorSet(listOf(it)) }

/** K で動かせる Processor（e2e-tests のビルドが jar を渡すもの）。それ以外を含むパターンは振り分けずに candidate のまま残す。 */
private val RUNNABLE = setOf(ProcessorId.CRM, ProcessorId.MSH, ProcessorId.KOI)

/** 1 パターンの判定。[status] は verified / rejected / skip（K で動かせない）/ error（道具やハーネスの不具合。candidate のまま） */
internal data class KVerdict(
    val id: String,
    val status: String,
    val note: String,
    val millis: Long,
) {
    fun tsv(): String = listOf(id, status, note.oneLine(), millis.toString()).joinToString("\t")
}

/** パターンを動かす Processor の組（並びは PROCESSORS の順）。動かせなければ null。 */
internal fun CatalogPattern.kSets(): List<ProcessorSet>? {
    if (background) return BACKGROUND_SETS
    val order = processorNames().mapNotNull { ProcessorId.fromCatalogName(it) }
    if (order.isEmpty() || order.any { it !in RUNNABLE } || order.size != processorNames().size) return null
    return listOf(ProcessorSet(order))
}

// index.json の processors は PROCESSORS の並びのまま。CatalogPattern.processors は Set なので並びを index から読み直す
private fun CatalogPattern.processorNames(): List<String> = catalogProcessorOrder[id].orEmpty()

private val catalogProcessorOrder: Map<String, List<String>> by lazy {
    val text = File(checkNotNull(catalog.baseDir), "index.json").readText()
    val root =
        Json
            .parseToJsonElement(text)
            .let { it as JsonObject }
    val patterns = root["patterns"] as JsonArray
    patterns.associate { e ->
        val o = e as JsonObject
        val id = (o["id"] as JsonPrimitive).content
        id to (o["processors"] as JsonArray).map { (it as JsonPrimitive).content }
    }
}

/** FILE の値の SourceSet（catalog.py の SrcFile.source_set と同じ規則）。 */
internal fun sourceSetOf(file: String): String {
    val parts = file.split("/")
    val suffixes = listOf("Main", "Test", "main", "test")
    return when {
        parts.size >= 3 && suffixes.any { parts[1].endsWith(it) } -> parts[1]
        parts.size >= 2 && (parts[0].endsWith("Main") || parts[0].endsWith("Test")) || parts[0] in setOf("main", "test") -> parts[0]
        else -> "main"
    }
}

/** K の結果が EXPECT どおりか（XD-02 と同じ基準）。合わなければ理由を返す。 */
internal fun CatalogPattern.mismatch(output: RunOutput): String? {
    val errors = output.diagnostics.filter { it.severity == DiagnosticRecord.Severity.Error }
    // Error が無いまま失敗したとき（例外で止まったなど）は、最後の診断を理由に使う
    val firstError =
        (errors.firstOrNull() ?: output.diagnostics.lastOrNull())
            ?.let { d -> (d.location?.let { "$it: " } ?: "") + d.message }
            .orEmpty()
    return when (expect) {
        Expect.OK, Expect.WARNING ->
            if (output.success) null else "K でビルドが失敗した: ${firstError.ifBlank { "（Error の診断なし）" }}"
        Expect.ERROR, Expect.COMPILE_ERROR ->
            when {
                output.success -> "K でビルドが通った（EXPECT: ${expect.name.lowercase().replace('_', '-')}）"
                expectMessage.isNotBlank() && output.diagnostics.none { expectMessage in it.message } ->
                    "K の失敗の文言に EXPECT_MESSAGE が無い: ${firstError.ifBlank { "（Error の診断なし）" }}"
                else -> null
            }
    }
}

internal fun verifyPattern(
    p: CatalogPattern,
    runner: KspReferenceRunner,
): KVerdict {
    val start = System.nanoTime()

    fun done(
        status: String,
        note: String,
    ) = KVerdict(p.id, status, note, (System.nanoTime() - start) / 1_000_000)
    val sets = p.kSets() ?: return done("skip", "K で動かせない Processor を含む: ${p.processorNames().joinToString(", ")}")
    p.files.map(::sourceSetOf).filter { it != "main" }.distinct().takeIf { it.isNotEmpty() }?.let {
        // 同じプロセス内の K は SourceSet main だけ（ADR-023）。KMP の配置は Gradle フィクスチャ経由のランナー（XD-18）で確かめる
        return done("skip", "main 以外の SourceSet を含む: ${it.joinToString(", ")}")
    }
    for (set in sets) {
        val run =
            try {
                runner.run(p.toDiffInput(set))
            } catch (e: Throwable) {
                return done("error", "${e::class.java.simpleName}: ${e.message}")
            }
        when (run) {
            is EngineRun.Completed -> {
                if (System.getProperty("acg.catalog.verbose") == "true") {
                    run.output.diagnostics.forEach { println("  [${p.id} $set] ${it.severity} ${it.location.orEmpty()} ${it.message}") }
                }
                p.mismatch(run.output)?.let { return done("rejected", if (p.background) "$set: $it" else it) }
            }
            is EngineRun.Unavailable -> return done("skip", "K が動かない: ${run.reason}")
        }
    }
    return done("verified", "")
}

/**
 * 引数: `<区画 i>/<区画数 n>` `<ID をカンマ区切り（空なら全件）>` `<出力 TSV>`。
 * 出力にすでに判定のある ID は飛ばす（止まっても同じコマンドで続きから動く）。
 */
public fun main(args: Array<String>) {
    val (shardArg, idsArg, outArg) = args.toList() + List(3 - args.size.coerceAtMost(3)) { "" }
    val (shard, shards) = shardArg.ifBlank { "0/1" }.split("/").map(String::toInt)
    val ids =
        idsArg
            .split(",")
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()
    val out = File(outArg.ifBlank { "build/acg/catalog-k.tsv" }).absoluteFile
    out.parentFile.mkdirs()
    val done =
        if (out.isFile) {
            out
                .readLines()
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .map { it.substringBefore("\t") }
                .toSet()
        } else {
            out.writeText("# id\tstatus\tnote\tmillis\n")
            emptySet()
        }
    val targets =
        catalog.patterns
            .filter { ids.isEmpty() || it.id in ids }
            .filter { Shards.of(it.id, shards) == shard }
            .filter { it.id !in done }
            .sortedBy { it.id }
    println("[verifyCatalog] 区画 $shard/$shards: ${targets.size} 件（済み ${done.size} 件）→ $out")
    val runner = KspReferenceRunner(FileKResultStore.default())
    val counts = sortedMapOf<String, Int>()
    targets.forEachIndexed { i, p ->
        val v = verifyPattern(p, runner)
        out.appendText(v.tsv() + "\n")
        counts.merge(v.status, 1, Int::plus)
        println("[verifyCatalog] ${i + 1}/${targets.size} ${v.id} ${v.status} ${v.millis}ms ${v.note.oneLine().take(160)}")
    }
    println("[verifyCatalog] 完了: $counts")
    // kct・AA が非 daemon のスレッドを残すことがあるので明示的に終える
    exitProcess(0)
}

private fun String.oneLine(): String = replace(Regex("\\s+"), " ").trim().take(400)

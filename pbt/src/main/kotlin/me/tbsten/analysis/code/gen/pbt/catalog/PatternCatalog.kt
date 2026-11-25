package me.tbsten.analysis.code.gen.pbt.catalog

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.tbsten.analysis.code.gen.pbt.processor.ProcessorId
import java.io.File

public enum class PatternStatus { CANDIDATE, VERIFIED, REJECTED }

public enum class Expect { OK, ERROR, WARNING, COMPILE_ERROR }

/** 入力パターンカタログ（ADR-020）の 1 パターン。index.json の 1 要素。 */
public data class CatalogPattern(
    val id: String,
    val title: String,
    val owner: String,
    val source: String,
    /** 空なら背景コード（`PROCESSORS: *`、どの組にも混ぜてよい） */
    val processors: Set<ProcessorId>,
    val background: Boolean,
    val expect: Expect,
    val expectMessage: String,
    val options: List<Map<String, String>>,
    val tags: Set<String>,
    val status: PatternStatus,
    val shard: Int,
    /** quality/catalog からの相対パス */
    val path: String,
    val files: List<String>,
) {
    val isError: Boolean get() = expect == Expect.ERROR || expect == Expect.COMPILE_ERROR
}

/**
 * index.json の読み込み口（ADR-020・ADR-022）。パターン本体のソースは [sources] で読む。
 * 形: `{"schema":1,"shards":24,"patterns":[{id,title,owner,source,processors,expect,expectMessage,options,tags,status,origin,shard,path,files}]}`
 */
public class PatternCatalog(
    public val patterns: List<CatalogPattern>,
    public val shards: Int,
    /** quality/catalog ディレクトリ（パターン本体を読むため）。メモリ上で作ったときは null */
    public val baseDir: File?,
) {
    /** 使ってよいパターン（rejected 以外）。 */
    public val usable: List<CatalogPattern> get() = patterns.filter { it.status != PatternStatus.REJECTED }

    /** Arb のランダム層に使うパターン（verified だけ。ADR-020 の 6 項）。 */
    public val verified: List<CatalogPattern> get() = patterns.filter { it.status == PatternStatus.VERIFIED }

    public fun sources(p: CatalogPattern): Map<String, String> {
        val dir = baseDir ?: return emptyMap()
        return splitFiles(File(dir, p.path).readText())
    }

    public companion object {
        /** 既定の置き場。`acg.repoRoot`（pbt の Test が渡す）→ カレントから上へ探す。 */
        public fun defaultIndexFile(): File? {
            System.getProperty("acg.repoRoot")?.let { root ->
                File(root, "quality/catalog/index.json").takeIf { it.isFile }?.let { return it }
            }
            var dir: File? = File("").absoluteFile
            while (dir != null) {
                val f = File(dir, "quality/catalog/index.json")
                if (f.isFile) return f
                dir = dir.parentFile
            }
            return null
        }

        /** index.json を読む。見つからなければ null（カタログが入る前のブランチ）。 */
        public fun loadOrNull(file: File? = defaultIndexFile()): PatternCatalog? = file?.let { parse(it.readText(), it.parentFile) }

        public fun parse(
            json: String,
            baseDir: File? = null,
        ): PatternCatalog {
            val root = Json.parseToJsonElement(json).jsonObject
            val shards = root["shards"]?.jsonPrimitive?.intOrNull ?: 24
            val patterns = root["patterns"]?.jsonArray.orEmpty().map { pattern(it.jsonObject) }
            return PatternCatalog(patterns, shards, baseDir)
        }

        private fun pattern(o: JsonObject): CatalogPattern {
            fun str(k: String) = (o[k] as? JsonPrimitive)?.contentOrNull.orEmpty()

            fun strs(k: String) = (o[k] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()
            val procs = strs("processors")
            return CatalogPattern(
                id = str("id"),
                title = str("title"),
                owner = str("owner"),
                source = str("source"),
                processors = procs.mapNotNull { ProcessorId.fromCatalogName(it) }.toSet(),
                background = "*" in procs,
                expect =
                    when (str("expect")) {
                        "error" -> Expect.ERROR
                        "warning" -> Expect.WARNING
                        "compile-error" -> Expect.COMPILE_ERROR
                        else -> Expect.OK
                    },
                expectMessage = str("expectMessage"),
                options = options(o["options"]),
                tags = strs("tags").toSet(),
                status =
                    when (str("status")) {
                        "verified" -> PatternStatus.VERIFIED
                        "rejected" -> PatternStatus.REJECTED
                        else -> PatternStatus.CANDIDATE
                    },
                shard = (o["shard"] as? JsonPrimitive)?.intOrNull ?: -1,
                path = str("path"),
                files = strs("files"),
            )
        }

        // ADR-020 の index.json は options を「オブジェクトの配列」で持つ。1 つのオブジェクトだけでも読めるようにしておく
        private fun options(e: JsonElement?): List<Map<String, String>> =
            when (e) {
                is JsonArray -> e.mapNotNull { (it as? JsonObject)?.toStringMap() }
                is JsonObject -> listOf(e.toStringMap()).filter { it.isNotEmpty() }
                else -> emptyList()
            }

        private fun JsonObject.toStringMap(): Map<String, String> =
            entries.associate { (k, v) -> k to ((v as? JsonPrimitive)?.contentOrNull ?: v.toString()) }

        /** パターン本体を `// FILE: <パス>` で分ける。ヘッダーのコメント行は捨てる。 */
        public fun splitFiles(text: String): Map<String, String> {
            val out = LinkedHashMap<String, StringBuilder>()
            var current: StringBuilder? = null
            for (line in text.lines()) {
                val m = FILE_MARKER.matchEntire(line.trim())
                if (m != null) {
                    current = StringBuilder().also { out[m.groupValues[1].trim()] = it }
                } else {
                    current?.appendLine(line)
                }
            }
            return out.mapValues { it.value.toString() }
        }

        private val FILE_MARKER = Regex("""//\s*FILE:\s*(.+)""")
    }
}

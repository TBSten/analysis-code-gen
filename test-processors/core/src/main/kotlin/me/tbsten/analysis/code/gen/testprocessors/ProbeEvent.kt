package me.tbsten.analysis.code.gen.testprocessors

import java.io.File

/**
 * TP-Probe が観測したこと 1 件。`tp.probe.out` のファイルに 1 行 1 件の JSON で追記する（ADR-027）。
 *
 * 生成 API を通さずに直接書くのは、`finish()` / `onError()` の中や Provider の生成時も記録したいため。
 *
 * @property event `create` / `process` / `finish` / `onError`
 * @property frontend `native` / `ksp`
 * @property instance Processor インスタンスの識別子（API-12 で同じかを見る）
 */
public data class ProbeEvent(
    val event: String,
    val frontend: String,
    val instance: String,
    val unit: String? = null,
    val round: Int? = null,
    val sourceSet: String? = null,
    val compilation: String? = null,
    val platform: String? = null,
    val options: Map<String, String> = emptyMap(),
    val allFiles: List<String> = emptyList(),
    val newFiles: List<String> = emptyList(),
    val changedFiles: List<String> = emptyList(),
    val removedFilePaths: List<String> = emptyList(),
    val javaFiles: List<String> = emptyList(),
    val deferred: List<String> = emptyList(),
    val extra: Map<String, String> = emptyMap(),
) {
    public fun toJson(): String =
        buildString {
            append('{')
            val fields = mutableListOf<String>()
            fields += "\"event\":${json(event)}"
            fields += "\"frontend\":${json(frontend)}"
            fields += "\"instance\":${json(instance)}"
            unit?.let { fields += "\"unit\":${json(it)}" }
            round?.let { fields += "\"round\":$it" }
            sourceSet?.let { fields += "\"sourceSet\":${json(it)}" }
            compilation?.let { fields += "\"compilation\":${json(it)}" }
            platform?.let { fields += "\"platform\":${json(it)}" }
            fields += "\"options\":${json(options)}"
            fields += "\"allFiles\":${json(allFiles)}"
            fields += "\"newFiles\":${json(newFiles)}"
            fields += "\"changedFiles\":${json(changedFiles)}"
            fields += "\"removedFilePaths\":${json(removedFilePaths)}"
            fields += "\"javaFiles\":${json(javaFiles)}"
            fields += "\"deferred\":${json(deferred)}"
            fields += "\"extra\":${json(extra)}"
            append(fields.joinToString(","))
            append('}')
        }

    public companion object {
        /** 1 行を追記する。複数の Processor・プロセスから書かれても行が混ざらないよう、ファイルをロックする。 */
        public fun append(
            path: String,
            event: ProbeEvent,
        ) {
            val file = File(path)
            file.parentFile?.mkdirs()
            java.io.RandomAccessFile(file, "rw").use { raf ->
                raf.channel.lock().use {
                    raf.seek(raf.length())
                    raf.write((event.toJson() + "\n").toByteArray(Charsets.UTF_8))
                }
            }
        }

        internal fun json(value: String): String =
            buildString {
                append('"')
                value.forEach { c ->
                    when {
                        c == '"' -> append("\\\"")
                        c == '\\' -> append("\\\\")
                        c == '\n' -> append("\\n")
                        c == '\r' -> append("\\r")
                        c == '\t' -> append("\\t")
                        c < ' ' -> append("\\u%04x".format(c.code))
                        else -> append(c)
                    }
                }
                append('"')
            }

        internal fun json(values: List<String>): String = values.joinToString(",", "[", "]") { json(it) }

        internal fun json(values: Map<String, String>): String =
            values.toSortedMap().entries.joinToString(",", "{", "}") { (k, v) -> "${json(k)}:${json(v)}" }
    }
}

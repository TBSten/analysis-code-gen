package me.tbsten.analysiscodegen.differential

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * K（KSP2）の結果のキャッシュ（pbt-differential-design.md 4 章、ADR-023、XD-17）。
 *
 * K は基準実装で、同じ入力なら毎回同じ結果になるので「入力のハッシュ → 出力」で持ち、同じ入力を何度も走らせない。
 * 鍵（[KCacheKey.of]）に入るもの: ソース（SourceSet・パス・中身）、Processor（id・版・実装クラスの置き場所のハッシュ・登録順）、
 * オプション、追加の classpath の中身、language version、Kotlin と KSP の版、キャッシュ形式の版。
 */
public interface KResultStore {
    public fun get(key: KCacheKey): RunOutput?

    public fun put(
        key: KCacheKey,
        output: RunOutput,
    )
}

/** K 結果の鍵。[hex] は SHA-256。[kotlinVersion] ごとにディレクトリを分ける（版を外すときにまとめて消せるように）。 */
public data class KCacheKey(val hex: String, val kotlinVersion: String) {
    public companion object {
        /** キャッシュ形式の版。[RunOutput] の形や記録の仕方を変えたら上げる（古いキャッシュを全部無効にする）。 */
        public const val FORMAT_VERSION: Int = 1

        public fun of(
            input: DiffInput,
            versions: EngineVersions,
        ): KCacheKey {
            val d = MessageDigest.getInstance("SHA-256")

            fun field(
                tag: String,
                value: String,
            ) {
                // 区切りの曖昧さを無くすため、名前と長さを前に付ける
                val bytes = value.toByteArray(Charsets.UTF_8)
                d.update("$tag:${bytes.size}:".toByteArray(Charsets.UTF_8))
                d.update(bytes)
            }
            field("format", FORMAT_VERSION.toString())
            field("kotlin", versions.kotlin)
            field("ksp", versions.ksp)
            field("languageVersion", input.languageVersion ?: "")
            input.sources.sortedWith(compareBy({ it.sourceSet }, { it.path })).forEach {
                field("source.set", it.sourceSet)
                field("source.path", it.path)
                field("source.content", it.content)
            }
            // 登録順は結果に効きうるので並べ替えない
            input.processors.forEach {
                field("processor.id", it.id)
                field("processor.version", it.version)
                field("processor.fingerprint", it.kspFingerprint)
            }
            input.options.toSortedMap().forEach { (k, v) ->
                field("option.key", k)
                field("option.value", v)
            }
            input.classpath.forEach { field("classpath", Fingerprints.ofFile(it)) }
            val hex = d.digest().joinToString("") { "%02x".format(it) }
            return KCacheKey(hex, versions.kotlin)
        }
    }
}

/** K 結果の鍵に入れる実行系の版。 */
public data class EngineVersions(val kotlin: String, val ksp: String) {
    public companion object {
        /**
         * 今の JVM で動く版を読む。Kotlin はコンパイラの `KotlinCompilerVersion`、KSP は `-Dacg.ksp.version`
         * （:differential のテストは catalog の値を渡す）か、KSP の jar の名前から読む。
         */
        public fun current(): EngineVersions =
            EngineVersions(
                kotlin = org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION,
                ksp = System.getProperty("acg.ksp.version") ?: kspVersionFromJar(),
            )

        private fun kspVersionFromJar(): String {
            val location =
                // ksp-impl は testing-kct の implementation なので、名前で読み込む
                runCatching { Class.forName("com.google.devtools.ksp.impl.KotlinSymbolProcessing") }
                    .getOrNull()
                    ?.protectionDomain
                    ?.codeSource
                    ?.location
                    ?.path
                    .orEmpty()
            return Regex("""symbol-processing[\w-]*?-(\d[\w.+-]*)\.jar""").find(location)?.groupValues?.get(1)
                ?: error("KSP の版が分からない。-Dacg.ksp.version を渡す（$location）")
        }
    }
}

/** メモリだけのキャッシュ。テストと 1 プロセス内の繰り返しに使う。 */
public class InMemoryKResultStore : KResultStore {
    private val map = ConcurrentHashMap<KCacheKey, RunOutput>()

    override fun get(key: KCacheKey): RunOutput? = map[key]

    override fun put(
        key: KCacheKey,
        output: RunOutput,
    ) {
        map[key] = output
    }

    public val size: Int get() = map.size
}

/**
 * ディレクトリに置くキャッシュ。`<dir>/<Kotlin の版>/<鍵の先頭 2 文字>/<鍵>.bin`（gzip）。
 * 書き込みは一時ファイル → rename なので、並列に書いても壊れたファイルは残らない。読めないファイルは無いものとして扱う。
 */
public class FileKResultStore(public val directory: File) : KResultStore {
    override fun get(key: KCacheKey): RunOutput? {
        val file = fileOf(key)
        if (!file.isFile) return null
        return try {
            DataInputStream(GZIPInputStream(file.inputStream().buffered())).use(RunOutputCodec::read)
        } catch (e: Exception) {
            null
        }
    }

    override fun put(
        key: KCacheKey,
        output: RunOutput,
    ) {
        val file = fileOf(key)
        file.parentFile.mkdirs()
        val tmp = File.createTempFile(key.hex, ".tmp", file.parentFile)
        DataOutputStream(GZIPOutputStream(tmp.outputStream().buffered())).use { RunOutputCodec.write(it, output) }
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun fileOf(key: KCacheKey) = directory.resolve(key.kotlinVersion).resolve(key.hex.take(2)).resolve("${key.hex}.bin")

    public companion object {
        /** `-Dacg.diff.cacheDir` があればそこ、無ければ一時ディレクトリの下。 */
        public fun default(): FileKResultStore =
            FileKResultStore(
                System.getProperty("acg.diff.cacheDir")?.let(::File)
                    ?: File(System.getProperty("java.io.tmpdir"), "acg-k-cache"),
            )
    }
}

/** [RunOutput] の読み書き（外部のライブラリを使わない単純な形式。形を変えたら [KCacheKey.FORMAT_VERSION] を上げる）。 */
internal object RunOutputCodec {
    private const val MAGIC = 0x41434B31 // "ACK1"

    fun write(
        out: DataOutputStream,
        o: RunOutput,
    ) {
        out.writeInt(MAGIC)
        out.writeBoolean(o.success)
        out.writeInt(o.files.size)
        o.files.forEach { (k, v) ->
            out.str(k)
            out.str(v)
        }
        out.writeInt(o.diagnostics.size)
        o.diagnostics.forEach {
            out.writeInt(it.severity.ordinal)
            out.str(it.message)
            out.str(it.location ?: "\u0000")
            out.writeBoolean(it.fromProcessor)
        }
        out.writeInt(o.rounds?.size ?: -1)
        o.rounds?.forEach { r ->
            out.writeInt(r.index)
            out.writeInt(r.generatedFiles.size)
            r.generatedFiles.forEach { out.str(it) }
        }
        out.writeInt(o.roots.size)
        o.roots.forEach { out.str(it) }
    }

    fun read(input: DataInputStream): RunOutput {
        check(input.readInt() == MAGIC) { "K 結果キャッシュの形式が違う" }
        val success = input.readBoolean()
        val files = (0 until input.readInt()).associate { input.str() to input.str() }.toSortedMap()
        val diagnostics =
            (0 until input.readInt()).map {
                DiagnosticRecord(
                    severity = DiagnosticRecord.Severity.entries[input.readInt()],
                    message = input.str(),
                    location = input.str().takeIf { it != "\u0000" },
                    fromProcessor = input.readBoolean(),
                )
            }
        val roundCount = input.readInt()
        val rounds =
            if (roundCount < 0) {
                null
            } else {
                (0 until roundCount).map { RoundRecord(input.readInt(), (0 until input.readInt()).map { input.str() }) }
            }
        val roots = (0 until input.readInt()).map { input.str() }
        return RunOutput(success, files, diagnostics, rounds, roots)
    }

    private fun DataOutputStream.str(s: String) {
        val b = s.toByteArray(Charsets.UTF_8)
        writeInt(b.size)
        write(b)
    }

    private fun DataInputStream.str(): String {
        val b = ByteArray(readInt())
        readFully(b)
        return String(b, Charsets.UTF_8)
    }
}

/** 中身のハッシュ。Processor の実装と追加の classpath の変化をキャッシュの鍵に反映するのに使う。 */
public object Fingerprints {
    private val cache = ConcurrentHashMap<String, String>()

    /** [type] が読み込まれた場所（jar かクラスのディレクトリ）の中身のハッシュ。場所が分からなければクラス名。 */
    public fun ofCodeSource(type: Class<*>): String {
        val location = type.protectionDomain?.codeSource?.location ?: return "class:${type.name}"
        return ofFile(File(location.toURI()))
    }

    /**
     * ファイルならその中身、ディレクトリなら中のファイル（相対パスと中身）全部のハッシュ。
     * ファイルは更新時刻と大きさが同じ間、ディレクトリ（ビルド中のクラス出力）は同じ JVM の中では 1 回だけ計算する。
     */
    public fun ofFile(file: File): String =
        cache.getOrPut(
            if (file.isDirectory) "dir:${file.absolutePath}" else "${file.absolutePath}@${file.lastModified()}@${file.length()}",
        ) {
            val d = MessageDigest.getInstance("SHA-256")
            if (file.isDirectory) {
                file.walkTopDown().filter { it.isFile }.sortedBy { it.relativeTo(file).invariantSeparatorsPath }.forEach {
                    d.update(it.relativeTo(file).invariantSeparatorsPath.toByteArray())
                    d.update(it.readBytes())
                }
            } else if (file.isFile) {
                d.update(file.readBytes())
            } else {
                d.update("missing:${file.path}".toByteArray())
            }
            d.digest().joinToString("") { "%02x".format(it) }
        }
}

package me.tbsten.analysis.code.gen.differential

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import java.io.File

/**
 * 三者差分の入力 1 件（ADR-023）。K / B / N の 3 つにまったく同じものを渡す。
 *
 * ```
 * val input = DiffInput(
 *     name = "mapper-1",
 *     sources = listOf(DiffSource("com/example/Foo.kt", "@Mapped class Foo(val a: Int)")),
 *     processors = listOf(DiffProcessor("TP", "1", ksp = { MapperKspProvider() })),
 *     options = mapOf("tp.processors" to "Mapper"),
 * )
 * DifferentialHarness.default().run(input).assertRequiredMatch()
 * ```
 *
 * - [sources] の並び・[options] の並びは結果にもキャッシュの鍵にも効かない（パスとキーで並べて扱う）
 * - [classpath] に足した jar とディレクトリは中身のハッシュを K 結果キャッシュの鍵に入れる。
 *   テストの JVM の classpath（`inheritClassPath`）は鍵に入れない（ADR-023 の割り切り）
 */
public data class DiffInput(
    val name: String,
    val sources: List<DiffSource>,
    val processors: List<DiffProcessor>,
    val options: Map<String, String> = emptyMap(),
    val expectation: Expectation = Expectation.Unknown,
    val classpath: List<File> = emptyList(),
    val languageVersion: String? = null,
) {
    init {
        require(name.isNotBlank()) { "DiffInput の name が空" }
        require(processors.isNotEmpty()) { "Processor を 1 つ以上渡す（$name）" }
        val duplicated = sources.groupBy { it.sourceSet to it.path }.filterValues { it.size > 1 }.keys
        require(duplicated.isEmpty()) { "同じパスのソースが重なっている（$name）: $duplicated" }
    }
}

/** 入力のソース 1 つ。[path] は SourceSet のソースルートからの相対パス（`com/example/Foo.kt`）。 */
public data class DiffSource(
    val path: String,
    val content: String,
    val sourceSet: String = MAIN,
) {
    init {
        require(path.endsWith(".kt") || path.endsWith(".java")) { "ソースは .kt か .java: $path" }
        require(!path.startsWith("/") && ".." !in path.split('/')) { "相対パスで書く: $path" }
    }

    public companion object {
        public const val MAIN: String = "main"
    }
}

/**
 * 比べる Processor 1 つ。
 *
 * - [ksp]: KSP 版の Provider を作る関数。K と B はこれを使う。無ければ K / B は動かない
 * - [nativeProviderClass]: Native 移植版の Provider のクラス名。analysis-code-gen-processor-api（ACG-34）が入るまでは型を持たずに名前で持つ。
 *   無い Processor（Room など）は N を動かさない
 * - [version]: 元の Processor の版（例: Moshi なら `1.15.2`）。K 結果キャッシュの鍵に入る。
 *   さらに [ksp] が作るクラスの置き場所（jar かディレクトリ）の中身のハッシュも鍵に入るので、版を上げ忘れてもキャッシュは古くならない
 */
public class DiffProcessor(
    public val id: String,
    public val version: String,
    public val ksp: (() -> SymbolProcessorProvider)? = null,
    public val nativeProviderClass: String? = null,
) {
    init {
        require(id.isNotBlank()) { "DiffProcessor の id が空" }
        require(ksp != null || nativeProviderClass != null) { "$id: KSP 版か Native 版のどちらかが要る" }
    }

    /** KSP 版の実装クラスの置き場所の中身のハッシュ。K 結果キャッシュの鍵に使う。KSP 版が無ければ空文字。 */
    public val kspFingerprint: String by lazy {
        val provider = ksp?.invoke() ?: return@lazy ""
        Fingerprints.ofCodeSource(provider::class.java)
    }

    override fun toString(): String = "DiffProcessor($id@$version)"
}

/** 入力が通るはずか（カタログの有効な入力 / わざと誤った入力）。XD-02・XD-08 が使う。 */
public enum class Expectation { Valid, Invalid, Unknown }

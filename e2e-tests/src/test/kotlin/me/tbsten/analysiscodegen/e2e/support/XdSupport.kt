package me.tbsten.analysiscodegen.e2e.support

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import me.tbsten.analysiscodegen.differential.BridgeRunner
import me.tbsten.analysiscodegen.differential.ComparisonPolicy
import me.tbsten.analysiscodegen.differential.DiffInput
import me.tbsten.analysiscodegen.differential.DiffProcessor
import me.tbsten.analysiscodegen.differential.DiffSource
import me.tbsten.analysiscodegen.differential.DifferentialHarness
import me.tbsten.analysiscodegen.differential.EngineKind
import me.tbsten.analysiscodegen.differential.Expectation
import me.tbsten.analysiscodegen.differential.FileKResultStore
import me.tbsten.analysiscodegen.differential.KspReferenceRunner
import me.tbsten.analysiscodegen.differential.NativeRunner
import me.tbsten.analysiscodegen.pbt.catalog.CatalogPattern
import me.tbsten.analysiscodegen.pbt.catalog.Expect
import me.tbsten.analysiscodegen.pbt.catalog.PatternCatalog
import me.tbsten.analysiscodegen.pbt.catalog.UserProject
import me.tbsten.analysiscodegen.pbt.processor.NativePort
import me.tbsten.analysiscodegen.pbt.processor.ProcessorId
import me.tbsten.analysiscodegen.pbt.processor.ProcessorSet
import me.tbsten.analysiscodegen.pbt.source.ProjectSpec
import me.tbsten.analysiscodegen.pbt.source.render
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader

// ACG-46 の補助（XD）。三者差分ハーネス（:differential、ADR-023）に実用 Processor とカタログの入力を渡す口。決まりは ADR-037

/** 入力パターンカタログ（quality/catalog/index.json、ADR-020）。 */
internal val catalog: PatternCatalog by lazy {
    PatternCatalog.loadOrNull(repoRoot.resolve("quality/catalog/index.json")) ?: error("quality/catalog/index.json が読めない")
}

/**
 * Native 移植版の Provider のクラス名（ADR-037）。移植版はこの名前で作る。
 * 任意（kotlin-inject・Dagger）は作ったときだけクラスが見つかり、見つからなければ N を動かさない。Room は移植しない。
 */
internal val NATIVE_PORT_PROVIDERS: Map<ProcessorId, String> =
    mapOf(
        ProcessorId.CRM to "me.tbsten.analysiscodegen.port.cream.CreamNativeProcessorProvider",
        ProcessorId.MSH to "me.tbsten.analysiscodegen.port.moshi.MoshiNativeProcessorProvider",
        ProcessorId.KOI to "me.tbsten.analysiscodegen.port.koin.KoinNativeProcessorProvider",
        ProcessorId.KIN to "me.tbsten.analysiscodegen.port.kotlininject.KotlinInjectNativeProcessorProvider",
        ProcessorId.DAG to "me.tbsten.analysiscodegen.port.dagger.DaggerNativeProcessorProvider",
    )

private fun nativePortExists(id: ProcessorId): Boolean =
    NATIVE_PORT_PROVIDERS[id]?.let { runCatching { Class.forName(it, false, XdLoader::class.java.classLoader) }.isSuccess } ?: false

private object XdLoader

/**
 * 実用 Processor の KSP 版を、その Processor の jar（と依存）だけを見る ClassLoader から ServiceLoader で読む。
 * jar の一覧は system property `acg.realProcessors.<ID>`（パス区切り）で渡す。e2e-tests のビルドが processors.toml の版で解決して渡す（ADR-037）
 */
internal fun loadRealKspProvider(id: ProcessorId): SymbolProcessorProvider {
    val paths = System.getProperty("acg.realProcessors.${id.name}")?.split(File.pathSeparator)?.filter { it.isNotBlank() }
    check(
        !paths.isNullOrEmpty(),
    ) { "system property acg.realProcessors.${id.name} が無い（e2e-tests のビルドで ${id.catalogName} の jar を渡す。ADR-037）" }
    val loader = URLClassLoader(paths.map { File(it).toURI().toURL() }.toTypedArray(), SymbolProcessorProvider::class.java.classLoader)
    val providers = ServiceLoader.load(SymbolProcessorProvider::class.java, loader).toList()
    check(providers.size == 1) { "${id.catalogName} の SymbolProcessorProvider が 1 つでない: ${providers.map { it::class.java.name }}" }
    return providers.single()
}

/** 実用 Processor 1 つ。版は processors.toml（ADR-019）、N は移植版があるときだけ。 */
internal fun realProcessor(id: ProcessorId): DiffProcessor =
    DiffProcessor(
        id = id.name,
        version = TargetProcessors.of(id.name).version,
        ksp = { loadRealKspProvider(id) },
        nativeProviderClass = NATIVE_PORT_PROVIDERS[id].takeIf { id.native != NativePort.NONE && nativePortExists(id) },
    )

/** N と比べることを求めるか: 組のすべてに必須の移植版があるとき（任意の移植版は作った場合だけ比べ、合格ラインに入れない）。 */
internal fun ProcessorSet.requiresNative(): Boolean = order.all { it.native == NativePort.REQUIRED }

internal fun Expect.toExpectation(): Expectation =
    when (this) {
        Expect.OK, Expect.WARNING -> Expectation.Valid
        Expect.ERROR, Expect.COMPILE_ERROR -> Expectation.Invalid
    }

/** カタログの入力を差分の入力にする。パターンのファイルは SourceSet main に置く。 */
internal fun UserProject.toDiffInput(
    set: ProcessorSet,
    options: Map<String, String> = preferredOptions.orEmpty(),
    name: String = "$this@$set",
): DiffInput =
    DiffInput(
        name = name,
        sources = patterns.flatMap { p -> catalog.sources(p).map { (path, content) -> DiffSource(path, content) } },
        processors = set.order.map(::realProcessor),
        options = options,
        expectation = expect.toExpectation(),
    )

internal fun CatalogPattern.toDiffInput(
    set: ProcessorSet,
    options: Map<String, String> = this.options.firstOrNull().orEmpty(),
): DiffInput = UserProject.of(this).toDiffInput(set, options, name = "$id@$set")

/** :pbt の ProjectSpec（単一モジュール）を差分の入力にする。 */
internal fun ProjectSpec.toDiffInput(
    set: ProcessorSet,
    options: Map<String, String>,
    name: String,
): DiffInput =
    DiffInput(
        name = name,
        sources =
            render(this).modules.first().files.map { (path, content) ->
                DiffSource(path.substringAfter("src/main/kotlin/").substringAfter("src/main/java/"), content)
            },
        processors = set.order.map(::realProcessor),
        options = options,
        expectation = Expectation.Valid,
    )

/** 比べる強さを指定したハーネス。K は K 結果キャッシュ付き（ADR-023）。 */
internal fun harness(
    bridge: ComparisonPolicy = ComparisonPolicy.forBridge(),
    native: ComparisonPolicy = ComparisonPolicy.forNative(),
): DifferentialHarness =
    DifferentialHarness(
        runners = listOf(KspReferenceRunner(FileKResultStore.default()), BridgeRunner(), NativeRunner()),
        policies = mapOf(EngineKind.B to bridge, EngineKind.N to native),
    )

/** 網羅の記録に使うラベル（XD-11 が 24 時間ぶんを集める）。 */
internal object CoverageLabels {
    fun pattern(id: String) = "pattern:$id"

    fun option(
        name: String,
        value: String,
    ) = "option:$name=$value"

    fun optionPair(
        a: Pair<String, String>,
        b: Pair<String, String>,
    ): String = listOf("${a.first}=${a.second}", "${b.first}=${b.second}").sorted().joinToString("&", prefix = "optionPair:")

    fun processorPair(set: ProcessorSet) = "processors:${set.order.joinToString("+")}"
}

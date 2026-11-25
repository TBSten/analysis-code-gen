package me.tbsten.analysiscodegen.enginetests.xd

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import me.tbsten.analysiscodegen.pbt.processor.ProcessorId
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorProvider
import me.tbsten.analysiscodegen.testing.CodegenTestResult
import me.tbsten.analysiscodegen.testing.CodegenTestSpec
import java.io.File
import java.lang.reflect.Modifier
import java.net.URLClassLoader
import java.util.ServiceLoader

// XD-05・XD-10 の補助（ACG-46）。三者差分の「段階 1」（公開 ABI と実行時テストの結果）を、testing の結果から取り出す。
// 決まりは ADR-037（XD）。e2e-tests の XdSupport と同じ渡し方をする（e2e-tests は AA の型に触れないので、こちらに別に置く）

/** Native 移植版の Provider のクラス名（ADR-037）。合格ラインに入るのは REQUIRED の 3 つ。 */
internal val NATIVE_PORT_PROVIDERS: Map<ProcessorId, String> =
    mapOf(
        ProcessorId.CRM to "me.tbsten.analysiscodegen.port.cream.CreamNativeProcessorProvider",
        ProcessorId.MSH to "me.tbsten.analysiscodegen.port.moshi.MoshiNativeProcessorProvider",
        ProcessorId.KOI to "me.tbsten.analysiscodegen.port.koin.KoinNativeProcessorProvider",
    )

/** 実用 Processor の JAR と実行時ライブラリ（system property `acg.realProcessors.<ID>`、パス区切り。ADR-037）。 */
internal fun realProcessorJars(id: ProcessorId): List<File> {
    val paths = System.getProperty("acg.realProcessors.${id.name}")?.split(File.pathSeparator)?.filter { it.isNotBlank() }
    check(!paths.isNullOrEmpty()) { "system property acg.realProcessors.${id.name} が無い（${id.catalogName} の JAR を渡す。ADR-037）" }
    return paths.map(::File)
}

/** 実用 Processor の KSP 版。その JAR だけを見る ClassLoader から読む。 */
internal fun realKspProvider(id: ProcessorId): SymbolProcessorProvider {
    val urls = realProcessorJars(id).map { it.toURI().toURL() }.toTypedArray()
    val loader = URLClassLoader(urls, SymbolProcessorProvider::class.java.classLoader)
    return ServiceLoader.load(SymbolProcessorProvider::class.java, loader).single()
}

/** Native 移植版の Provider。 */
internal fun nativePortProvider(id: ProcessorId): AnalysisCodegenProcessorProvider {
    val name = NATIVE_PORT_PROVIDERS[id] ?: error("${id.catalogName} の Native 移植版は合格ラインの対象外（ADR-037）")
    return Class.forName(name).getDeclaredConstructor().newInstance() as AnalysisCodegenProcessorProvider
}

/** カタログ・PBT のソース（ソースルートからの相対パス → 中身）と、実用 Processor の実行時ライブラリを入れる。 */
internal fun CodegenTestSpec.loadSources(
    sources: Map<String, String>,
    processors: List<ProcessorId>,
) {
    sources.forEach { (path, content) -> if (path.endsWith(".java")) java(path, content) else kotlin(path, content) }
    processors.forEach { classpath += realProcessorJars(it) }
}

/** コンパイル結果のクラスの出力ディレクトリ（testing の ClassLoader は「出力 + classpath」の順、ADR-021）。 */
private val CodegenTestResult.classesDir: File get() = File((classLoader as URLClassLoader).urLs.first().toURI())

/**
 * コンパイル結果の公開 ABI（段階 1）。public / protected のクラスとメンバーのシグネチャを辞書順に並べた文字列。
 * private・合成メンバー・Kotlin のメタデータの中身は比べない（同じ公開面なら利用者から見て同じ）。
 */
internal fun CodegenTestResult.publicAbi(): List<String> {
    val visible = { m: Int -> Modifier.isPublic(m) || Modifier.isProtected(m) }
    return classesDir
        .walkTopDown()
        .filter { it.isFile && it.extension == "class" }
        .map {
            it
                .relativeTo(classesDir)
                .invariantSeparatorsPath
                .removeSuffix(".class")
                .replace('/', '.')
        }.map(::loadClass)
        .filter { visible(it.modifiers) && !it.isSynthetic }
        .flatMap { c ->
            val header = "${Modifier.toString(c.modifiers)} ${c.name} : ${c.genericSuperclass} ${c.genericInterfaces.toList()}"
            val members =
                c.declaredFields.filter { visible(it.modifiers) && !it.isSynthetic }.map { it.toGenericString() } +
                    c.declaredConstructors.filter { visible(it.modifiers) && !it.isSynthetic }.map { it.toGenericString() } +
                    c.declaredMethods.filter { visible(it.modifiers) && !it.isSynthetic && !it.isBridge }.map { it.toGenericString() }
            listOf(header) + members.map { "  $it" }.sorted()
        }.sorted()
        .toList()
}

/**
 * 実行時テスト（段階 1）。パターンに `Check.kt`（`fun check(): String`）があれば呼んだ結果を返す。無ければ null（ABI だけを比べる）。
 * 置き方は ADR-037（XD）。
 */
internal fun CodegenTestResult.runtimeCheck(sources: Map<String, String>): String? {
    val check = sources.entries.firstOrNull { it.key.substringAfterLast('/') == "Check.kt" } ?: return null
    val pkg =
        Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
            .find(check.value)
            ?.groupValues
            ?.get(1)
    val facade = if (pkg == null) "CheckKt" else "$pkg.CheckKt"
    return loadClass(facade).getMethod("check").invoke(null) as String
}

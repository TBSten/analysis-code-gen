package me.tbsten.analysis.code.gen.testing.internal

import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.spi.FileSystemProvider
import java.util.Enumeration
import java.util.zip.ZipFile

/**
 * kct（kotlin-compiler-embeddable）と KSP2 を、AA（*-for-ide）と分けて動かすための classpath と ClassLoader（ADR-061・065）。
 *
 * 両者は `org.jetbrains.kotlin.cli.*` などを同じ名前で持ち、AA を先頭に置いたテストの JVM では kct・KSP2 が壊れる。
 * AA と再配置されていない IntelliJ を除いた classpath だけで別の ClassLoader を作る。
 */
internal object IsolatedClasspath {
    init {
        // JDK の FileSystemProvider の一覧を、分けた ClassLoader に入る前に読んでおく。
        // 初めての読み込みが分けた ClassLoader の中（KSP2 の jrt ファイルシステム）で起きると
        // "Circular loading of installed providers detected" で落ち、以後ずっと読めなくなるため
        FileSystemProvider.installedProviders()
    }

    /** テストの JVM の classpath。 */
    fun host(): List<File> =
        System
            .getProperty("java.class.path")
            .split(File.pathSeparator)
            .filter { it.isNotBlank() }
            .map(::File)
            .filter { it.exists() }

    /** AA と IntelliJ Platform（再配置されていないもの）を除いた classpath。kct と embeddable と KSP2 と、その依存が残る。 */
    val entries: List<File> by lazy { host().filter(::isolatable) }

    private fun isolatable(entry: File): Boolean {
        if (entry.isDirectory) return true
        if ("-for-ide" in entry.name) return false
        return runCatching {
            ZipFile(entry).use { zip ->
                // org/jetbrains/kotlin/analysis/ は見ない。kotlin-compiler-embeddable 2.4.0 も（再配置せずに）持っているため
                zip.entries().asSequence().none { it.name.startsWith("com/intellij/") }
            }
        }.getOrDefault(true)
    }

    private fun urls(): Array<URL> = entries.map { it.toURI().toURL() }.toTypedArray()

    /** 何も共有しない ClassLoader（親は platform）。型をまたがないものをリフレクションで呼ぶ。 */
    val standalone: ClassLoader by lazy { URLClassLoader(urls(), ClassLoader.getPlatformClassLoader()) }

    /**
     * テストの型（spec・結果）・KSP の公開 API・kotlin 標準ライブラリだけを呼び出し元と共有する ClassLoader。
     * KSP の Provider のインスタンスをそのまま KSP2 に渡すため（ADR-065）。
     */
    val sharingTestApi: ClassLoader by lazy {
        SharingClassLoader(urls(), IsolatedClasspath::class.java.classLoader, ::sharedWithHost)
    }

    private fun sharedWithHost(name: String): Boolean =
        when {
            name.startsWith("kotlin.") -> true
            // KSP の公開 API（symbol-processing-api と common-deps）だけ。impl・standalone などの実装は KSP2 側で読む
            name.startsWith(KSP_PACKAGE) -> name.substringAfter(KSP_PACKAGE).substringBeforeLast('.', "") in KSP_API_PACKAGES
            name.startsWith(KCT_PACKAGE) -> false
            name.startsWith(INTERNAL_PACKAGE) -> name.substringAfter(INTERNAL_PACKAGE).substringBefore('$') !in ISOLATED_RUNNERS
            name.startsWith(TESTING_PACKAGE) -> true
            name.startsWith(PROCESSOR_PACKAGE) -> true
            else -> false
        }

    private const val KSP_PACKAGE = "com.google.devtools.ksp."
    private const val TESTING_PACKAGE = "me.tbsten.analysis.code.gen.testing."
    private const val KCT_PACKAGE = "${TESTING_PACKAGE}kct."
    private const val INTERNAL_PACKAGE = "${TESTING_PACKAGE}internal."
    private const val PROCESSOR_PACKAGE = "me.tbsten.analysis.code.gen.processor."
    private val ISOLATED_RUNNERS = setOf("Ksp2TestRunner")
    private val KSP_API_PACKAGES = setOf("", "processing", "symbol", "visitor", "errors")
}

/** [shared] に当たる名前だけ [host] から読み、それ以外は自分の classpath を先に読む（child-first）。 */
private class SharingClassLoader(
    urls: Array<URL>,
    private val host: ClassLoader,
    private val shared: (String) -> Boolean,
) : URLClassLoader(urls, ClassLoader.getPlatformClassLoader()) {
    override fun loadClass(
        name: String,
        resolve: Boolean,
    ): Class<*> {
        if (shared(name)) return host.loadClass(name)
        return super.loadClass(name, resolve)
    }

    override fun getResource(name: String): URL? = super.getResource(name) ?: host.getResource(name)

    override fun getResources(name: String): Enumeration<URL> = super.getResources(name)
}

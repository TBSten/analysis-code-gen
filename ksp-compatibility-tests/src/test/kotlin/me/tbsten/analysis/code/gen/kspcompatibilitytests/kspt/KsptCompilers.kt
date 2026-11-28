package me.tbsten.analysis.code.gen.kspcompatibilitytests.kspt

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import java.util.zip.ZipFile

/**
 * KSPT のライブラリのモジュールをコンパイルする kotlinc（kotlin-compiler-embeddable）を、AA（`*-for-ide`）と分けた ClassLoader で動かす。
 *
 * Bridge（B）を動かすため、テストの実行時クラスパスの先頭に AA の一式を置いている（ADR-060）。AA の `*-for-ide` と
 * kotlin-compiler-embeddable は `org.jetbrains.kotlin.cli.*` を同じ名前で持つので、同じ ClassLoader では kotlinc が
 * for-ide 側を読んでしまう。testing の AnalysisCodegenTestRunner（ADR-061）と同じく、AA と IntelliJ を除いた ClassLoader で呼ぶ。
 */
object KsptCompilers {
    private val loader: ClassLoader by lazy {
        val entries =
            System
                .getProperty("java.class.path")
                .split(File.pathSeparator)
                .filter { it.isNotBlank() }
                .map(::File)
                .filter { it.exists() && isolatable(it) }
        URLClassLoader(entries.map { it.toURI().toURL() }.toTypedArray(), ClassLoader.getPlatformClassLoader())
    }

    /** AA と IntelliJ Platform（再配置されていないもの）を含む JAR を除く。 */
    private fun isolatable(entry: File): Boolean {
        if (entry.isDirectory) return true
        if ("-for-ide" in entry.name) return false
        return runCatching {
            ZipFile(entry).use { zip ->
                zip.entries().asSequence().none {
                    it.name.startsWith("com/intellij/") || it.name.startsWith("org/jetbrains/kotlin/analysis/")
                }
            }
        }.getOrDefault(true)
    }

    /** `K2JVMCompiler`（JVM のライブラリ）。 */
    fun jvm(args: List<String>) = exec("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler", args)

    /** `KotlinMetadataCompiler`（Native のライブラリを KLIB のメタデータにする）。 */
    fun metadata(args: List<String>) = exec("org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler", args)

    /** 上流（AbstractKSPAATest.runCompiler）と同じく `exec(PrintStream, String[])` を呼ぶ。失敗したら例外。 */
    private fun exec(
        compilerClass: String,
        args: List<String>,
    ) {
        val compiler = Class.forName(compilerClass, true, loader).getDeclaredConstructor().newInstance()
        val out = ByteArrayOutputStream()
        val exec = compiler.javaClass.getMethod("exec", PrintStream::class.java, Array<String>::class.java)
        val code = exec.invoke(compiler, PrintStream(out), args.toTypedArray())
        check(code.toString() == "OK") { "Kotlin のコンパイルに失敗した（$code）:\n$out" }
    }
}

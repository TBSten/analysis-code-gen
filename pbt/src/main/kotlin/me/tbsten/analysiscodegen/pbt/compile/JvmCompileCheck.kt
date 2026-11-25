package me.tbsten.analysiscodegen.pbt.compile

import me.tbsten.analysiscodegen.pbt.source.RenderedProject
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import javax.tools.ToolProvider

/** コンパイルの結果。[ok] でなければ [log] に kotlinc / javac の出力が入る。 */
public data class CompileResult(
    val ok: Boolean,
    val log: String,
)

/**
 * 書き出したプロジェクトを、モジュールの依存順に kotlinc → javac でコンパイルする（PBT-01）。
 *
 * Gradle を使わずプロセス内でコンパイルするので、1 件あたり 1 秒前後で済む。
 * Kotlin の版は `pbt` モジュールが依存する kotlin-compiler-embeddable（= catalog の `kotlin`）。
 */
public object JvmCompileCheck {
    private val stdlib: String by lazy {
        File(
            Unit::class.java.protectionDomain.codeSource.location
                .toURI(),
        ).absolutePath
    }

    public fun compile(
        project: RenderedProject,
        workDir: File = Files.createTempDirectory("acg-pbt-compile").toFile(),
    ): CompileResult {
        try {
            val outputs = HashMap<String, File>()
            val log = StringBuilder()
            for (m in project.modules) {
                val src = File(workDir, "${m.name}/src").apply { mkdirs() }
                val out = File(workDir, "${m.name}/classes").apply { mkdirs() }
                outputs[m.name] = out
                val files =
                    m.files.map { (path, content) ->
                        File(src, path).apply {
                            parentFile.mkdirs()
                            writeText(content)
                        }
                    }
                if (files.isEmpty()) continue
                val deps = transitiveDeps(project, m.name).mapNotNull { outputs[it] }
                val classpath = (listOf(stdlib) + deps.map { it.absolutePath }).joinToString(File.pathSeparator)
                val ktFiles = files.filter { it.extension == "kt" }
                val javaFiles = files.filter { it.extension == "java" }

                val buf = ByteArrayOutputStream()
                val args =
                    listOf(
                        "-d",
                        out.absolutePath,
                        "-classpath",
                        classpath,
                        "-no-stdlib",
                        "-no-reflect",
                        "-jvm-target",
                        "17",
                        "-module-name",
                        m.name,
                        "-nowarn",
                    ) + (ktFiles + javaFiles).map { it.absolutePath }
                if (ktFiles.isNotEmpty()) {
                    val code = K2JVMCompiler().exec(PrintStream(buf), *args.toTypedArray())
                    if (code != ExitCode.OK) {
                        log.appendLine("[kotlinc ${m.name}] exit=$code").append(buf.toString())
                        return CompileResult(false, log.toString())
                    }
                }
                if (javaFiles.isNotEmpty()) {
                    val javac = ToolProvider.getSystemJavaCompiler() ?: error("javac が見つからない（JDK で実行する）")
                    val jbuf = ByteArrayOutputStream()
                    val jcp = (listOf(stdlib, out.absolutePath) + deps.map { it.absolutePath }).joinToString(File.pathSeparator)
                    val jargs =
                        listOf("-d", out.absolutePath, "-classpath", jcp, "-nowarn", "--release", "17") + javaFiles.map { it.absolutePath }
                    val code = javac.run(null, jbuf, jbuf, *jargs.toTypedArray())
                    if (code != 0) {
                        log.appendLine("[javac ${m.name}] exit=$code").append(jbuf.toString())
                        return CompileResult(false, log.toString())
                    }
                }
            }
            return CompileResult(true, log.toString())
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun transitiveDeps(
        project: RenderedProject,
        name: String,
    ): Set<String> {
        val byName = project.modules.associateBy { it.name }
        val out = LinkedHashSet<String>()

        fun walk(n: String) {
            byName[n]?.dependsOn?.forEach { if (out.add(it)) walk(it) }
        }
        walk(name)
        return out
    }
}

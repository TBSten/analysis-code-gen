package me.tbsten.analysiscodegen.testing

import java.io.File

/** 診断 1 件。 */
public data class CodegenDiagnostic(val severity: Severity, val message: String) {
    public enum class Severity { Error, Warning, Info, Logging }
}

/**
 * [runCodegenTest] の結果。
 *
 * - [generatedFiles] は生成されたソースとリソース。キーは出力先ルートからの相対パス（`kotlin/…`・`java/…`・`resources/…`）で、
 *   辞書順に並ぶ（決定性。三者差分でそのまま比べられる）
 * - [isSuccess] は生成とコンパイルの両方が通ったか
 * - [classLoader] で生成物を含むコンパイル結果を読み込んで動かせる（TST-03）
 */
public class CodegenTestResult internal constructor(
    public val engine: TestEngine,
    public val isSuccess: Boolean,
    public val generatedFiles: Map<String, String>,
    public val diagnostics: List<CodegenDiagnostic>,
    /** コンパイラと Processor の出力をそのまま並べたもの。失敗の調査用。 */
    public val messages: String,
    private val outputDirectory: File?,
    private val classpath: List<File>,
) {
    /** エラーの診断。 */
    public val errors: List<CodegenDiagnostic> get() = diagnostics.filter { it.severity == CodegenDiagnostic.Severity.Error }

    /** 警告の診断。 */
    public val warnings: List<CodegenDiagnostic> get() = diagnostics.filter { it.severity == CodegenDiagnostic.Severity.Warning }

    /** 生成物を含むコンパイル結果のクラスローダー。コンパイルに失敗したときは [IllegalStateException]。 */
    public val classLoader: ClassLoader by lazy {
        check(isSuccess && outputDirectory != null) { "コンパイルに失敗したのでクラスを読み込めない:\n$messages" }
        java.net.URLClassLoader(
            (listOf(outputDirectory) + classpath).map { it.toURI().toURL() }.toTypedArray(),
            CodegenTestResult::class.java.classLoader,
        )
    }

    /** 生成物を含むコンパイル結果からクラスを読み込む。 */
    public fun loadClass(fqName: String): Class<*> = classLoader.loadClass(fqName)

    /** パスの末尾が [suffix] に一致する生成ファイルの中身。無ければ [NoSuchElementException]。 */
    public fun generatedFile(suffix: String): String =
        generatedFiles.entries.singleOrNull { it.key == suffix || it.key.endsWith("/$suffix") }?.value
            ?: throw NoSuchElementException("生成ファイル $suffix が無い。生成されたもの: ${generatedFiles.keys}")

    override fun toString(): String =
        "CodegenTestResult(engine=$engine, isSuccess=$isSuccess, generated=${generatedFiles.keys}, errors=$errors)"
}

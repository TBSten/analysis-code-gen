package me.tbsten.analysis.code.gen.testing

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorProvider
import me.tbsten.analysis.code.gen.processor.ProcessingScope
import java.io.File

/** テストに入れる 1 ファイル。[path] は SourceSet のソースルートからの相対パス（例: `com/example/Foo.kt`）。 */
public data class TestSourceFile(val path: String, val content: String) {
    init {
        require(path.endsWith(".kt") || path.endsWith(".java")) { "ソースは .kt か .java: $path" }
        require(!path.startsWith("/") && ".." !in path.split('/')) { "相対パスで書く: $path" }
    }
}

/** テストで動かす Processor。 */
public sealed interface TestProcessor {
    /** KSP の Processor。[TestEngine.Ksp2] では公式 KSP2 で、[TestEngine.AnalysisCodegen] では Bridge で動く（TST-02）。 */
    public data class Ksp(val provider: SymbolProcessorProvider) : TestProcessor

    /** Native の Processor。[TestEngine.AnalysisCodegen] でだけ動く（ADR-013・021）。 */
    public data class Native(val provider: AnalysisCodegenProcessorProvider) : TestProcessor

    /**
     * Processor の JAR（Gradle の component 1 つに当たる）。中の `META-INF/services` から Native / KSP の Provider を探す。
     * 同じ JAR に両方あるときの選び方は ADR-017（Native だけを使う）。クラス本体はテストの classpath からも読める。
     */
    public data class Jar(val file: File) : TestProcessor
}

/**
 * KMP の SourceSet（TST-04）。[dependsOn] は上流の SourceSet 名。
 * 既定の構成は `main` 1 つ（kotlin("jvm") 相当）。
 */
public class TestSourceSet internal constructor(public val name: String) {
    public val dependsOn: MutableList<String> = mutableListOf()

    /** friend の関係にある SourceSet（`internal` が見える。commonTest → commonMain など）。 */
    public val friendOf: MutableList<String> = mutableListOf()

    /** この SourceSet を実行単位にしたときの Processing Scope（ADR-013）。 */
    public var processingScope: ProcessingScope = ProcessingScope.Own
    public val files: MutableList<TestSourceFile> = mutableListOf()

    /** Kotlin ファイルを足す。 */
    public fun kotlin(
        path: String,
        content: String,
    ) {
        files += TestSourceFile(path.withExtension(".kt"), content.trimIndent())
    }

    /** Java ファイルを足す。 */
    public fun java(
        path: String,
        content: String,
    ) {
        files += TestSourceFile(path.withExtension(".java"), content.trimIndent())
    }

    private fun String.withExtension(ext: String) = if (endsWith(ext)) this else "$this$ext"
}

/**
 * [runCodegenTest] の入力。DSL で組み立てる。
 *
 * ```
 * runCodegenTest(TestEngine.Ksp2) {
 *     kotlin("com/example/Foo.kt", "@Hello class Foo")
 *     ksp(HelloProcessorProvider())
 *     options["hello.prefix"] = "Hi"
 * }
 * ```
 */
public class CodegenTestSpec internal constructor() {
    internal val sourceSets: LinkedHashMap<String, TestSourceSet> = linkedMapOf(MAIN to TestSourceSet(MAIN))

    /** 動かす Processor（登録順に渡す）。 */
    public val processors: MutableList<TestProcessor> = mutableListOf()

    /** Processor に渡すオプション（ADR-013）。 */
    public val options: MutableMap<String, String> = linkedMapOf()

    /** 追加の classpath（ライブラリの jar やクラスのディレクトリ）。 */
    public val classpath: MutableList<File> = mutableListOf()

    /** テストを動かしている JVM の classpath を、コンパイル対象からも見えるようにする。既定は true。 */
    public var inheritClassPath: Boolean = true

    /** Kotlin の language version（例: `2.4`）。null ならコンパイラの既定。 */
    public var languageVersion: String? = null

    /** 生成物と中間ファイルを置く場所。null なら一時ディレクトリ。 */
    public var workingDir: File? = null

    /** 最大ラウンド数（ADR-014。DSL の `maxRounds`）。null ならエンジンの既定（100）。[TestEngine.AnalysisCodegen] でだけ効く。 */
    public var maxRounds: Int? = null

    /** 最終ラウンドで deferred が残ったらエラーにするか（DSL の `unresolvedDeferred`）。false なら警告。 */
    public var failOnUnresolvedDeferred: Boolean = true

    /** `main` SourceSet に Kotlin ファイルを足す。 */
    public fun kotlin(
        path: String,
        content: String,
    ): Unit = sourceSet(MAIN) { kotlin(path, content) }

    /** `main` SourceSet に Java ファイルを足す。 */
    public fun java(
        path: String,
        content: String,
    ): Unit = sourceSet(MAIN) { java(path, content) }

    /** KSP Processor を足す。 */
    public fun ksp(provider: SymbolProcessorProvider) {
        processors += TestProcessor.Ksp(provider)
    }

    /** Native Processor を足す。 */
    public fun native(provider: AnalysisCodegenProcessorProvider) {
        processors += TestProcessor.Native(provider)
    }

    /** Processor の JAR を足す（[TestProcessor.Jar]）。 */
    public fun processorJar(file: File) {
        processors += TestProcessor.Jar(file)
    }

    /**
     * SourceSet を作る（あれば取り出す）。KMP のテスト用（TST-04）。
     *
     * ```
     * sourceSet("commonMain") { kotlin("A.kt", "...") }
     * sourceSet("jvmMain") { dependsOn += "commonMain"; kotlin("B.kt", "...") }
     * ```
     */
    public fun sourceSet(
        name: String,
        block: TestSourceSet.() -> Unit,
    ) {
        sourceSets.getOrPut(name) { TestSourceSet(name) }.block()
    }

    internal fun isSingleSourceSet(): Boolean = sourceSets.values.count { it.files.isNotEmpty() || it.name != MAIN } <= 1

    internal fun copy(): CodegenTestSpec =
        CodegenTestSpec().also { c ->
            c.sourceSets.clear()
            sourceSets.forEach { (name, ss) ->
                c.sourceSets[name] =
                    TestSourceSet(name).also { n ->
                        n.dependsOn += ss.dependsOn
                        n.friendOf += ss.friendOf
                        n.processingScope = ss.processingScope
                        n.files += ss.files
                    }
            }
            c.processors += processors
            c.options += options
            c.classpath += classpath
            c.inheritClassPath = inheritClassPath
            c.languageVersion = languageVersion
            c.workingDir = workingDir
            c.maxRounds = maxRounds
            c.failOnUnresolvedDeferred = failOnUnresolvedDeferred
        }

    public companion object {
        /** 既定の SourceSet 名。 */
        public const val MAIN: String = "main"
    }
}

package me.tbsten.analysis.code.gen.e2e.brg

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import me.tbsten.analysis.code.gen.testing.CodegenTestResult
import me.tbsten.analysis.code.gen.testing.CodegenTestSpec
import me.tbsten.analysis.code.gen.testing.TestEngine
import me.tbsten.analysis.code.gen.testing.runCodegenTest

// BRG・BINC・MIX・HYB が共有する、KSP API の結果を文字列に落として比べるための補助（ACG-45、ADR-034）。
// 同じ入力を TestEngine.Ksp2（K）と TestEngine.AnalysisCodegen（Bridge）で動かし、出てきた文字列を比べる

/** ラウンド 1 の [dump] の結果を、リソース `inspect/<name>.txt` に書く Processor。 */
class InspectProvider(
    private val name: String = "result",
    private val dump: (resolver: Resolver, environment: SymbolProcessorEnvironment) -> String,
) : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        object : SymbolProcessor {
            private var round = 0

            override fun process(resolver: Resolver): List<KSAnnotated> {
                round++
                if (round == 1) environment.codeGenerator.writeText("inspect/$name", "txt", dump(resolver, environment))
                return emptyList()
            }
        }
}

/** 依存なしでリソースを書く。 */
fun CodeGenerator.writeText(
    path: String,
    extension: String,
    text: String,
) {
    createNewFileByPath(Dependencies(false), path, extension).use { it.write(text.toByteArray()) }
}

/** [InspectProvider] で [dump] を回し、書かれた文字列を返す。 */
fun inspect(
    engine: TestEngine,
    sources: CodegenTestSpec.() -> Unit,
    dump: (Resolver, SymbolProcessorEnvironment) -> String,
): String {
    val result =
        runCodegenTest(engine) {
            sources()
            ksp(InspectProvider(dump = dump))
        }
    check(result.isSuccess) { "生成かコンパイルに失敗した: $result\n${result.messages}" }
    return result.generatedFile("inspect/result.txt")
}

/** 同じ入力を KSP2 と Bridge で動かした結果の組。 */
data class KspPair(val ksp2: String, val bridge: String)

/** [inspect] を KSP2 と Bridge の両方で回す。 */
fun inspectBoth(
    sources: CodegenTestSpec.() -> Unit,
    dump: (Resolver, SymbolProcessorEnvironment) -> String,
): KspPair = KspPair(inspect(TestEngine.Ksp2, sources, dump), inspect(TestEngine.AnalysisCodegen, sources, dump))

/** 生成物とコンパイル結果を、エンジンをまたいで比べられる形にする（生成物のパスと中身・成否・エラー）。 */
fun CodegenTestResult.comparable(): String =
    buildString {
        appendLine("success=$isSuccess")
        errors.forEach { appendLine("error=${it.message.lineSequence().first()}") }
        generatedFiles.forEach { (path, content) ->
            appendLine("=== $path")
            appendLine(content)
        }
    }

/** BRG-02・03・09・10 で共有する入力: 宣言の種類・ジェネリクス・typealias・関数型・ネスト・Java を一通り含む。 */
val brgSources: CodegenTestSpec.() -> Unit = {
    kotlin(
        "com/example/Model.kt",
        """
        package com.example

        annotation class Marker(val value: String = "")

        typealias Callback<T> = (T) -> Unit
        typealias Ids = List<Long>

        interface Shape { val area: Double }

        @Marker("data")
        data class Box<out T : Comparable<@UnsafeVariance T>>(val value: T, val ids: Ids) : Shape {
            override val area: Double get() = 0.0
            inner class Inner
            companion object { const val NAME = "box" }
        }

        @Marker
        sealed class Result {
            data class Ok(val value: Int) : Result()
            object Fail : Result()
        }

        @Marker("fn")
        fun <T> transform(items: List<T>, block: Callback<T>, star: List<*>, suspendFn: suspend (Int) -> String): Map<String, T>? = null

        @Marker("prop")
        val topLevel: Box<String>? = null

        enum class Color { RED, GREEN }
        """,
    )
    kotlin("com/example/other/Other.kt", "package com.example.other\n\nclass Other\nfun helper() = 1")
    java(
        "com/example/JavaModel.java",
        """
        package com.example;

        @Marker("java")
        public class JavaModel<T extends Number> {
            public java.util.List<? extends T> items;
            public static int count(java.util.Map<String, ? super Integer> map) { return 0; }
        }
        """,
    )
}

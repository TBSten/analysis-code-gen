package me.tbsten.analysiscodegen.e2e.pbt

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import me.tbsten.analysiscodegen.pbt.run.acgCheckAll
import me.tbsten.analysiscodegen.pbt.source.ProjectShape
import me.tbsten.analysiscodegen.pbt.source.ProjectSpec
import me.tbsten.analysiscodegen.pbt.source.projectSpec
import me.tbsten.analysiscodegen.pbt.source.render
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PbtSpec
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import me.tbsten.analysiscodegen.testing.CodegenTestResult
import me.tbsten.analysiscodegen.testing.TestEngine
import me.tbsten.analysiscodegen.testing.runCodegenTest

// PBT-03: ランダムな入力（:pbt の projectSpec）× ランダムな Resolver API の呼び出し列で、Bridge と KSP2 の結果が一致する
// 呼び出し列はオプション `trace.calls` で渡し、ResolverTraceProcessor が結果を文字列に書き出して生成物（resources/trace.txt）にする。
// 生成物の比較になるので、testing の runCodegenTest で K と B を同じように流せる

/** Resolver の呼び出し 1 つ。引数は入力のプロジェクトから選んだ名前。 */
internal sealed interface ResolverCall {
    fun encode(): String

    data object AllFiles : ResolverCall {
        override fun encode() = "files"
    }

    data class SymbolsWithAnnotation(val fqn: String) : ResolverCall {
        override fun encode() = "annotated:$fqn"
    }

    data class ClassByName(val fqn: String) : ResolverCall {
        override fun encode() = "class:$fqn"
    }

    data class FunctionsByName(val fqn: String) : ResolverCall {
        override fun encode() = "functions:$fqn"
    }

    data class DeclarationsFromPackage(val pkg: String) : ResolverCall {
        override fun encode() = "package:$pkg"
    }

    companion object {
        fun decode(s: String): ResolverCall =
            when (s.substringBefore(':')) {
                "files" -> AllFiles
                "annotated" -> SymbolsWithAnnotation(s.substringAfter(':'))
                "class" -> ClassByName(s.substringAfter(':'))
                "functions" -> FunctionsByName(s.substringAfter(':'))
                "package" -> DeclarationsFromPackage(s.substringAfter(':'))
                else -> error("未知の呼び出し: $s")
            }
    }
}

/** 呼び出し列を順に実行し、結果を 1 呼び出し 1 ブロックで書き出す KSP Processor。最初のラウンドだけ動く。 */
internal class ResolverTraceProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        ResolverTraceProcessor(environment.codeGenerator, environment.options)
}

// getDeclarationsFromPackage は KSP では実験的 API。Bridge でも同じ結果を返す必要があるので呼び出し列に入れる
@OptIn(KspExperimental::class)
private class ResolverTraceProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: Map<String, String>,
) : SymbolProcessor {
    private var done = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (done) return emptyList()
        done = true
        val calls =
            options["trace.calls"]
                .orEmpty()
                .split('|')
                .filter { it.isNotEmpty() }
                .map(ResolverCall::decode)
        val trace =
            buildString {
                for (call in calls) {
                    appendLine("## ${call.encode()}")
                    for (line in run(resolver, call)) appendLine(line)
                }
            }
        codeGenerator.createNewFile(Dependencies(aggregating = true), "", "trace", "txt").use { it.write(trace.toByteArray()) }
        return emptyList()
    }

    private fun run(
        resolver: Resolver,
        call: ResolverCall,
    ): List<String> =
        when (call) {
            ResolverCall.AllFiles -> resolver.getAllFiles().map { "${it.packageName.asString()}/${it.fileName}" }.toList()
            is ResolverCall.SymbolsWithAnnotation -> resolver.getSymbolsWithAnnotation(call.fqn).map { describeRef(it) }.toList()
            is ResolverCall.ClassByName -> {
                val cls = resolver.getClassDeclarationByName(resolver.getKSNameFromString(call.fqn))
                if (cls == null) listOf("null") else describeClass(cls)
            }
            is ResolverCall.FunctionsByName ->
                resolver
                    .getFunctionDeclarationsByName(resolver.getKSNameFromString(call.fqn), includeTopLevel = true)
                    .map { fn ->
                        "${fn.simpleName.asString()}(${fn.parameters.joinToString {
                            "${it.name?.asString()}: ${it.type.resolve()}"
                        }}): ${fn.returnType?.resolve()}"
                    }.toList()
            is ResolverCall.DeclarationsFromPackage -> resolver.getDeclarationsFromPackage(call.pkg).map { describeRef(it) }.toList()
        }

    private fun describeRef(a: KSAnnotated): String =
        when (a) {
            is KSDeclaration -> "${a::class.simpleName?.removePrefix(
                "KS",
            )?.removeSuffix("Impl")}:${a.qualifiedName?.asString() ?: a.simpleName.asString()}"
            else -> a.toString()
        }

    private fun describeClass(cls: KSClassDeclaration): List<String> =
        listOf(
            "kind=${cls.classKind} modifiers=${cls.modifiers.map { it.name }.sorted()}",
            "typeParameters=${cls.typeParameters.map {
                "${it.variance} ${it.name.asString()} : ${it.bounds.map { b ->
                    b.resolve()
                }.toList()}"
            }}",
            "supertypes=${cls.superTypes.map { it.resolve().toString() }.toList()}",
            "annotations=${cls.annotations.map { it.shortName.asString() }.toList()}",
            "functions=${cls.getDeclaredFunctions().map { it.simpleName.asString() }.toList()}",
            "properties=${cls.getDeclaredProperties().map { "${it.simpleName.asString()}: ${it.type.resolve()}" }.toList()}",
            "nested=${cls.declarations.filterIsInstance<KSClassDeclaration>().map { it.simpleName.asString() }.toList()}",
        )
}

/** 書き出したプロジェクトから、呼び出しの引数に使う名前（パッケージ・クラス・関数・注釈）を拾う。 */
internal data class NamePool(
    val packages: List<String>,
    val classes: List<String>,
    val functions: List<String>,
    val annotations: List<String>,
) {
    companion object {
        private val PACKAGE = Regex("""(?m)^package\s+([\w.]+)""")
        private val CLASS =
            Regex(
                """(?m)^(?:public |internal |private )?(?:abstract |open |sealed |data |value |enum |annotation )*(?:class|interface|object)\s+(\w+)""",
            )
        private val ANNOTATION = Regex("""(?m)^(?:public |internal |private )?annotation class\s+(\w+)""")
        private val FUNCTION = Regex("""(?m)^(?:public |internal |private )?fun\s+(?:<[^>]*>\s*)?(\w+)""")

        fun of(sources: Map<String, String>): NamePool {
            val kotlin = sources.filterKeys { it.endsWith(".kt") }.values

            fun fq(
                regex: Regex,
                text: String,
            ): List<String> {
                val pkg =
                    PACKAGE
                        .find(text)
                        ?.groupValues
                        ?.get(1)
                        .orEmpty()
                return regex.findAll(text).map { if (pkg.isEmpty()) it.groupValues[1] else "$pkg.${it.groupValues[1]}" }.toList()
            }
            return NamePool(
                packages =
                    kotlin
                        .mapNotNull { PACKAGE.find(it)?.groupValues?.get(1) }
                        .distinct()
                        .sorted()
                        .ifEmpty { listOf("") },
                classes = kotlin.flatMap { fq(CLASS, it) }.distinct().sorted() + "does.not.Exist",
                functions = kotlin.flatMap { fq(FUNCTION, it) }.distinct().sorted() + "does.not.exist",
                annotations = kotlin.flatMap { fq(ANNOTATION, it) }.distinct().sorted() + "kotlin.Deprecated",
            )
        }
    }

    /** 整数の列から呼び出し列を作る（縮小は整数の列で効く）。 */
    fun calls(picks: List<Int>): List<ResolverCall> =
        picks.map { p ->
            val n = p / 5
            when (p % 5) {
                0 -> ResolverCall.AllFiles
                1 -> ResolverCall.SymbolsWithAnnotation(annotations[n % annotations.size])
                2 -> ResolverCall.ClassByName(classes[n % classes.size])
                3 -> ResolverCall.FunctionsByName(functions[n % functions.size])
                else -> ResolverCall.DeclarationsFromPackage(packages[n % packages.size])
            }
        }
}

/** 単一モジュールの入力を書き出す（パスは SourceSet のソースルートからの相対にする）。 */
internal fun ProjectSpec.singleModuleSources(): Map<String, String> =
    render(this)
        .modules
        .first()
        .files
        .mapKeys { (path, _) -> path.substringAfter("src/main/kotlin/").substringAfter("src/main/java/") }

private fun trace(
    engine: TestEngine,
    sources: Map<String, String>,
    calls: List<ResolverCall>,
): CodegenTestResult =
    runCodegenTest(engine) {
        sources.forEach { (path, content) -> if (path.endsWith(".java")) java(path, content) else kotlin(path, content) }
        ksp(ResolverTraceProcessorProvider())
        options["trace.calls"] = calls.joinToString("|") { it.encode() }
    }

class ResolverDifferentialPbtTest : PbtSpec({
    case("PBT-03", "ランダムな入力とランダムな Resolver API の呼び出し列で、Bridge と KSP2 の結果が一致する", pending = PendingCase(Milestone.M2)) {
        val arb = Arb.bind(Arb.projectSpec(ProjectShape.SMALL), Arb.list(Arb.int(0..999), 1..8)) { spec, picks -> spec to picks }
        acgCheckAll("PBT-03 Resolver の呼び出し列の結果が Bridge と KSP2 で一致する", arb, weight = 2.0) { (spec, picks) ->
            val sources = spec.singleModuleSources()
            val calls = NamePool.of(sources).calls(picks)
            val k = trace(TestEngine.Ksp2, sources, calls)
            val b = trace(TestEngine.AnalysisCodegen, sources, calls)
            withClue("calls=${calls.map { it.encode() }}") {
                k.isSuccess shouldBe true
                b.isSuccess shouldBe true
                b.generatedFile("trace.txt") shouldBe k.generatedFile("trace.txt")
            }
        }
    }
})

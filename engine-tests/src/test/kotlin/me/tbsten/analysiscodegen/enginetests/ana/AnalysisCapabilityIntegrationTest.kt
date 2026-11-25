package me.tbsten.analysiscodegen.enginetests.ana

import io.kotest.matchers.shouldBe
import me.tbsten.analysiscodegen.enginetests.support.processorProvider
import me.tbsten.analysiscodegen.enginetests.support.runNative
import me.tbsten.analysiscodegen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysiscodegen.testfoundation.IntegrationSpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

private val M1 = PendingCase(Milestone.M1)
private val M2 = PendingCase(Milestone.M2)

/** ラウンド 1 にだけ [block] を AA のセッションで動かし、その戻り値を返す。 */
private fun <R> analyzeRound1(
    setup: me.tbsten.analysiscodegen.testing.CodegenTestSpec.() -> Unit,
    block: KaSession.(AnalysisCodegenRoundContext) -> R,
): R {
    var out: Result<R>? = null
    val result =
        runNative {
            setup()
            native(
                processorProvider { ctx ->
                    if (ctx.round == 1) out = runCatching { analyze(ctx.sourceSet.module) { block(ctx) } }
                },
            )
        }
    check(out != null) { "Processor が呼ばれなかった: ${result.messages}" }
    return out!!.getOrThrow()
}

private fun cid(fqn: String) = ClassId(FqName(fqn.substringBeforeLast('.')), Name.identifier(fqn.substringAfterLast('.')))

/** 型を `kotlin.collections.List<kotlin.String>?` の形で書く（star は `*`、エラー型は `<error>`）。 */
private fun KaSession.text(type: KaType): String {
    val t = type.fullyExpandedType
    val q = if (t.isMarkedNullable) "?" else ""
    return when (t) {
        is KaErrorType -> "<error>"
        // Java の型（プラットフォーム型）は下限（非 null 側）で書く
        is KaFlexibleType -> text(t.lowerBound)
        is KaClassType -> {
            val args =
                t.typeArguments.map {
                    when (it) {
                        is KaStarTypeProjection -> "*"
                        is KaTypeArgumentWithVariance -> text(it.type)
                    }
                }
            t.classId.asFqNameString() + (if (args.isEmpty()) "" else args.joinToString(", ", "<", ">")) + q
        }
        else -> t.toString() + q
    }
}

// 5.4 AA ネイティブな解析能力（ANA）のうち I のケース
class AnalysisCapabilityIntegrationTest : IntegrationSpec({
    case("ANA-02", "ライブラリ（JAR / klib）の宣言と型を解決できる", M1) {
        // JAR: テストの classpath（inheritClassPath）にある kotest と JDK。klib は KMP-07（E2E）で見る
        val (members, jdk) =
            analyzeRound1({ kotlin("a/A.kt", "package a\nclass A") }) {
                val matcher = findClass(cid("io.kotest.matchers.Matcher"))!!
                val test = matcher.memberScope.callables(Name.identifier("test")).single()
                val chm = findClass(cid("java.util.concurrent.ConcurrentHashMap"))!!
                listOf(matcher.classKind.name, text(test.returnType)) to (chm.origin == KaSymbolOrigin.JAVA_LIBRARY)
            }
        members shouldBe listOf("INTERFACE", "io.kotest.matchers.MatcherResult")
        jdk shouldBe true
    }

    case("ANA-03", "Java ソースの宣言を KaSymbol として扱える", M1) {
        val seen =
            analyzeRound1({
                kotlin("a/A.kt", "package a\nclass A")
                java(
                    "j/Person.java",
                    "package j;\npublic class Person {\n  public String getName() { return \"\"; }\n  public static int count;\n}",
                )
            }) { ctx ->
                val person = findClass(cid("j.Person")) as KaNamedClassSymbol
                listOf(
                    person.origin.name,
                    person.memberScope
                        .callables(Name.identifier("getName"))
                        .single()
                        .let { text(it.returnType) },
                    person.staticMemberScope
                        .callables(Name.identifier("count"))
                        .single()
                        .let { text(it.returnType) },
                    ctx.allJavaFiles.single().name,
                )
            }
        seen shouldBe listOf("JAVA_SOURCE", "kotlin.String", "kotlin.Int", "Person.java")
    }

    case("ANA-04", "ジェネリクス・型エイリアス・nullable・star projection・関数型を解決できる", M1) {
        val types =
            analyzeRound1({
                kotlin(
                    "a/Types.kt",
                    """
                    package a
                    typealias Names = List<String>
                    class Box<T : Comparable<T>>(val value: T)
                    class Holder(
                        val generic: Map<String, List<Int?>>,
                        val alias: Names,
                        val nullable: Box<String>?,
                        val star: List<*>,
                        val fn: (Int, String) -> Boolean,
                        val suspendFn: suspend () -> Unit,
                    )
                    """,
                )
            }) { ctx ->
                val holder =
                    ctx.allFiles
                        .single()
                        .collectDescendantsOfType<KtClassOrObject>()
                        .single { it.name == "Holder" }
                val props =
                    (holder.symbol as KaClassSymbol)
                        .memberScope.callables
                        .filterIsInstance<org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol>()
                props.associate { it.name.asString() to text(it.returnType) } +
                    (
                        "fnIsFunctionType" to
                            ((props.single { it.name.asString() == "fn" }.returnType as? KaFunctionType)?.parameterTypes?.size.toString())
                    ) +
                    (
                        "suspend" to (
                            (
                                props
                                    .single {
                                        it.name.asString() == "suspendFn"
                                    }.returnType as? KaFunctionType
                            )?.isSuspend.toString()
                        )
                    )
            }
        types["generic"] shouldBe "kotlin.collections.Map<kotlin.String, kotlin.collections.List<kotlin.Int?>>"
        types["alias"] shouldBe "kotlin.collections.List<kotlin.String>"
        types["nullable"] shouldBe "a.Box<kotlin.String>?"
        types["star"] shouldBe "kotlin.collections.List<*>"
        types["fn"] shouldBe "kotlin.Function2<kotlin.Int, kotlin.String, kotlin.Boolean>"
        types["fnIsFunctionType"] shouldBe "2"
        types["suspend"] shouldBe "true"
    }

    case("ANA-05", "アノテーション引数（ネスト、配列、enum、KClass、repeatable、use-site target）を読める", M1) {
        val seen =
            analyzeRound1({
                kotlin(
                    "a/Ann.kt",
                    """
                    package a
                    import kotlin.reflect.KClass
                    enum class Level { LOW, HIGH }
                    annotation class Inner(val name: String)
                    @Repeatable annotation class Tag(val value: String)
                    annotation class Full(val inner: Inner, val names: Array<String>, val level: Level, val type: KClass<*>)
                    @Target(AnnotationTarget.FIELD) annotation class OnField

                    @Full(inner = Inner("n"), names = ["x", "y"], level = Level.HIGH, type = String::class)
                    @Tag("a") @Tag("b")
                    class Target(@field:OnField val p: Int)
                    """,
                )
            }) { ctx ->
                val target = findClass(cid("a.Target"))!!
                val full = target.annotations[cid("a.Full")].single()
                val args = full.arguments.associate { it.name.asString() to it.expression }
                val inner = (args.getValue("inner") as KaAnnotationValue.NestedAnnotationValue).annotation
                val names =
                    (
                        args.getValue(
                            "names",
                        ) as KaAnnotationValue.ArrayValue
                    ).values.map { (it as KaAnnotationValue.ConstantValue).value.value }
                val level = (args.getValue("level") as KaAnnotationValue.EnumEntryValue).callableId?.callableName?.asString()
                val type = (args.getValue("type") as KaAnnotationValue.ClassLiteralValue).classId?.asFqNameString()
                val tags =
                    target.annotations[
                        cid(
                            "a.Tag",
                        ),
                    ].map { (it.arguments.single().expression as KaAnnotationValue.ConstantValue).value.value }
                val ctorParam =
                    ctx.allFiles.single().collectDescendantsOfType<org.jetbrains.kotlin.psi.KtParameter>().single {
                        it.name ==
                            "p"
                    }
                val useSite =
                    ctorParam.annotationEntries
                        .single()
                        .useSiteTarget
                        ?.getAnnotationUseSiteTarget()
                        ?.name
                listOf(
                    (inner.arguments.single().expression as KaAnnotationValue.ConstantValue).value.value,
                    names,
                    level,
                    type,
                    tags,
                    useSite,
                )
            }
        seen shouldBe listOf("n", listOf("x", "y"), "HIGH", "kotlin.String", listOf("a", "b"), "FIELD")
    }

    case("ANA-06", "未解決参照などエラーを含むソースでも解析が落ちず、エラー型として扱える", M1) {
        var processed = false
        val types =
            analyzeRound1({
                kotlin("a/Broken.kt", "package a\nclass Broken(val missing: DoesNotExist, val ok: Int) {\n  fun f() = undefinedCall()\n}")
            }) { ctx ->
                processed = true
                val cls =
                    ctx.allFiles
                        .single()
                        .collectDescendantsOfType<KtClassOrObject>()
                        .single()
                val props =
                    (cls.symbol as KaClassSymbol)
                        .memberScope.callables
                        .filterIsInstance<org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol>()
                val call =
                    ctx.allFiles
                        .single()
                        .collectDescendantsOfType<KtCallExpression>()
                        .single()
                props.associate { it.name.asString() to text(it.returnType) } +
                    ("call" to (call.resolveToCall() == null || call.expressionType is KaErrorType).toString())
            }
        processed shouldBe true
        types shouldBe mapOf("missing" to "<error>", "ok" to "kotlin.Int", "call" to "true")
    }

    case("ANA-07", "構文だけの処理（PSI）と意味解析（AA）を同じ Processor で混ぜて使える", M1) {
        val seen =
            analyzeRound1({
                kotlin("a/Mix.kt", "package a\nclass Mix {\n  fun twice(x: Int) = x * 2\n  fun name() = \"n\"\n}")
            }) { ctx ->
                // PSI で関数を集め（構文）、AA で戻り値の型を推論する（意味）
                ctx.allFiles
                    .single()
                    .collectDescendantsOfType<KtNamedFunction>()
                    .filter { it.typeReference == null }
                    .associate { it.name!! to text(it.symbol.returnType) }
            }
        seen shouldBe mapOf("twice" to "kotlin.Int", "name" to "kotlin.String")
    }

    case("ANA-08", "対応する各 Kotlin バージョンの言語機能（context parameters、value class、sealed interface、data object など）を含むソースを解析できる", M2) {
        // context parameters はコンパイラフラグが要るため GRD-06（Gradle のフラグが解析に反映される）で見る
        val seen =
            analyzeRound1({
                kotlin(
                    "a/Features.kt",
                    """
                    package a
                    @JvmInline value class Id(val raw: Long)
                    sealed interface Shape
                    data object Empty : Shape
                    data class Circle(val r: Double) : Shape
                    enum class Dir { N, S }
                    fun Shape.area(): Double = when (this) { Empty -> 0.0; is Circle -> r * r }
                    """,
                )
            }) {
                val id = findClass(cid("a.Id")) as KaNamedClassSymbol
                val shape = findClass(cid("a.Shape")) as KaNamedClassSymbol
                val empty = findClass(cid("a.Empty")) as KaNamedClassSymbol
                val dir = findClass(cid("a.Dir")) as KaNamedClassSymbol
                listOf(
                    id.isInline,
                    shape.classKind == KaClassKind.INTERFACE && shape.modality == org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality.SEALED,
                    shape.sealedClassInheritors.map { it.classId!!.shortClassName.asString() }.sorted() == listOf("Circle", "Empty"),
                    empty.classKind == KaClassKind.OBJECT && empty.isData,
                    dir.staticDeclaredMemberScope
                        .callables(Name.identifier("entries"))
                        .toList()
                        .isNotEmpty(),
                )
            }
        seen shouldBe listOf(true, true, true, true, true)
    }

    case("ANA-10", "KDoc とコメントを取得できる", M2) {
        val seen =
            analyzeRound1({
                kotlin(
                    "a/Doc.kt",
                    """
                    package a
                    /**
                     * ユーザー。
                     * @property name 名前
                     */
                    class User(val name: String) {
                        // 行コメント
                        /* ブロックコメント */
                        fun f() = 1
                    }
                    """,
                )
            }) { ctx ->
                val cls =
                    ctx.allFiles
                        .single()
                        .collectDescendantsOfType<KtClassOrObject>()
                        .single()
                val kdoc = cls.docComment!!
                val comments =
                    cls
                        .collectDescendantsOfType<com.intellij.psi.PsiComment>()
                        .filter {
                            it !is org.jetbrains.kotlin.kdoc.psi.api.KDoc
                        }.map { it.text }
                listOf(
                    kdoc.getDefaultSection().getContent().trim(),
                    kdoc
                        .getDefaultSection()
                        .findTagByName("property")
                        ?.getContent()
                        ?.trim(),
                    comments,
                )
            }
        seen shouldBe listOf("ユーザー。", "名前", listOf("// 行コメント", "/* ブロックコメント */"))
    }
})

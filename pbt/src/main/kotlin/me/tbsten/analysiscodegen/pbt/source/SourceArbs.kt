package me.tbsten.analysiscodegen.pbt.source

import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.arbitrary
import kotlin.random.Random

/**
 * ランダムな利用者プロジェクトの大きさ。kotest の試行が進むほど大きくしたいときは、呼び出し側で段階を変える。
 */
public data class ProjectShape(
    val maxModules: Int = 3,
    val maxFilesPerModule: Int = 3,
    val maxDeclsPerFile: Int = 4,
    val maxMembers: Int = 3,
    val maxNestingDepth: Int = 2,
    /** Java ファイルを混ぜる確率 */
    val javaRatio: Double = 0.25,
) {
    public companion object {
        public val SMALL: ProjectShape = ProjectShape(maxModules = 1, maxFilesPerModule = 2, maxDeclsPerFile = 3, maxMembers = 2)
        public val DEFAULT: ProjectShape = ProjectShape()
    }
}

/**
 * ランダムな Kotlin / Java プロジェクトの Arb（PBT-01 の対象）。
 *
 * 宣言の種類（class / data / interface / object / enum / sealed / value / annotation / typealias / 関数 / プロパティ）、
 * 可視性、ジェネリクス（変位・上限）、nullable、注釈（use-site target 付き）、入れ子・inner、companion、
 * 複数ファイル・複数モジュール、Java 混在を組み合わせる。出力は必ず [normalize] を通すので常に妥当。
 * 縮小は [ProjectShrinker]。edgecase は空のプロジェクトと、各宣言の種類を 1 つずつ持つ小さなプロジェクト。
 */
public fun Arb.Companion.projectSpec(shape: ProjectShape = ProjectShape.DEFAULT): Arb<ProjectSpec> =
    arbitrary(edgecases = projectEdgecases(), shrinker = ProjectShrinker) { rs -> generateProject(rs.random, shape) }

/** 生成器の版。生成の仕方を変えたら上げる（保存した seed が同じ値を再現しなくなるため。ADR-022 の 4 節）。 */
public const val SOURCE_GENERATOR_VERSION: Int = 1

public fun generateProject(
    random: Random,
    shape: ProjectShape = ProjectShape.DEFAULT,
): ProjectSpec = normalize(Gen(random, shape).project())

private fun projectEdgecases(): List<ProjectSpec> {
    val empty = normalize(ProjectSpec(listOf(ModuleSpec("m0", emptyList(), listOf(KotlinFileSpec("F0", "gen.m0.p0", emptyList()))))))
    val kinds =
        ClassKind.entries.map { k ->
            normalize(
                ProjectSpec(
                    listOf(
                        ModuleSpec(
                            "m0",
                            emptyList(),
                            listOf(KotlinFileSpec("F0", "gen.m0.p0", listOf(KClassDecl("C0", kind = k, ctorParams = listOf(ParamSpec("p0", TypeRef.INT, PropKind.VAL)))))),
                        ),
                    ),
                ),
            )
        }
    return listOf(empty) + kinds
}

/** 生成中に参照できる宣言。 */
private class Ref(
    val id: String,
    val module: String,
    val kind: ClassKind?,
    val typeParams: Int,
)

private class Gen(
    private val r: Random,
    private val shape: ProjectShape,
) {
    private var counter = 0
    private val refs = mutableListOf<Ref>()
    private val javaRefs = mutableListOf<Pair<String, String>>()
    private var module = "m0"

    private fun next(prefix: String) = "$prefix${counter++}"

    private fun chance(p: Double) = r.nextDouble() < p

    private fun <T> pick(list: List<T>): T = list[r.nextInt(list.size)]

    fun project(): ProjectSpec {
        val n = 1 + r.nextInt(shape.maxModules)
        val modules = mutableListOf<ModuleSpec>()
        for (mi in 0 until n) {
            module = "m$mi"
            val deps = modules.map { it.name }.filter { chance(0.6) }
            val files = mutableListOf<SourceFileSpec>()
            val fileCount = 1 + r.nextInt(shape.maxFilesPerModule)
            repeat(fileCount) { fi ->
                val pkg = "gen.$module.p${r.nextInt(2)}"
                if (chance(shape.javaRatio)) {
                    files += javaFile(pkg)
                } else {
                    val decls = List(r.nextInt(shape.maxDeclsPerFile + 1)) { top() }
                    files += KotlinFileSpec("F$fi", pkg, decls)
                }
            }
            modules += ModuleSpec(module, deps, files)
        }
        return ProjectSpec(modules)
    }

    private fun javaFile(pkg: String): JavaFileSpec {
        val id = next("J")
        val fields = List(r.nextInt(3)) { JFieldSpec(next("jf"), javaFieldType()) }
        javaRefs += id to module
        return JavaFileSpec(pkg, JClassDecl(id, fields = fields))
    }

    private fun javaFieldType(): TypeRef {
        val candidates = refs.filter { it.typeParams == 0 && it.kind in setOf(ClassKind.CLASS, ClassKind.DATA, ClassKind.INTERFACE, ClassKind.ENUM) }
        return when {
            candidates.isNotEmpty() && chance(0.5) -> TypeRef.Decl(pick(candidates).id)
            javaRefs.isNotEmpty() && chance(0.3) -> TypeRef.Java(pick(javaRefs).first)
            else -> TypeRef.Builtin(pick(listOf(BuiltinType.INT, BuiltinType.STRING, BuiltinType.BOOLEAN, BuiltinType.LONG)))
        }
    }

    private fun visibility(): Visibility =
        when (r.nextInt(10)) {
            0 -> Visibility.PRIVATE
            1, 2 -> Visibility.INTERNAL
            else -> Visibility.PUBLIC
        }

    private fun top(): KTopLevel =
        when (r.nextInt(10)) {
            0 -> KTypeAlias(next("A"), visibility = visibility(), target = type(emptyList(), 0))
            1 -> {
                val fn = function(emptyList())
                KTopFunction(next("tf"), fn, receiver = if (chance(0.3)) type(fn.typeParams, 0) else null)
            }
            2 -> KTopProperty(next("tp"), property(emptyList()), isConst = chance(0.3))
            else -> cls(depth = 0, parentKind = null)
        }

    private fun classKind(): ClassKind =
        when (r.nextInt(16)) {
            0, 1, 2, 3, 4 -> ClassKind.CLASS
            5, 6 -> ClassKind.DATA
            7, 8 -> ClassKind.INTERFACE
            9 -> ClassKind.OBJECT
            10 -> ClassKind.ENUM
            11, 12 -> ClassKind.SEALED
            13 -> ClassKind.VALUE
            else -> ClassKind.ANNOTATION
        }

    private fun typeParams(): List<TypeParamSpec> {
        if (!chance(0.3)) return emptyList()
        return List(1 + r.nextInt(2)) { i ->
            val variance =
                when (r.nextInt(4)) {
                    0 -> Variance.OUT
                    1 -> Variance.IN
                    else -> Variance.INVARIANT
                }
            val ifaces = refs.filter { it.kind == ClassKind.INTERFACE && it.typeParams == 0 }
            val bound =
                when (r.nextInt(5)) {
                    0 -> Bound.AnyBound
                    1 -> Bound.ComparableSelf
                    2 -> if (ifaces.isNotEmpty()) Bound.Interface(pick(ifaces).id) else Bound.None
                    else -> Bound.None
                }
            TypeParamSpec("T$i", variance, bound)
        }
    }

    fun type(
        tps: List<TypeParamSpec>,
        depth: Int,
    ): TypeRef {
        val nullable = chance(0.25)
        return when (r.nextInt(if (depth > 1) 6 else 9)) {
            0, 1 -> if (tps.isNotEmpty()) TypeRef.Param(pick(tps).name, nullable) else TypeRef.Builtin(BuiltinType.STRING, nullable = nullable)
            2, 3 -> {
                val cands = refs.filter { it.kind != ClassKind.ANNOTATION }
                if (cands.isNotEmpty()) TypeRef.Decl(pick(cands).id, nullable = nullable) else TypeRef.Builtin(BuiltinType.INT, nullable = nullable)
            }
            4 -> if (javaRefs.isNotEmpty()) TypeRef.Java(pick(javaRefs).first) else TypeRef.Builtin(BuiltinType.LONG, nullable = nullable)
            5 -> TypeRef.Builtin(pick(listOf(BuiltinType.INT, BuiltinType.STRING, BuiltinType.BOOLEAN, BuiltinType.DOUBLE, BuiltinType.LONG)), nullable = nullable)
            else -> {
                val t = pick(listOf(BuiltinType.LIST, BuiltinType.SET, BuiltinType.MAP))
                TypeRef.Builtin(t, List(t.arity) { type(tps, depth + 1) }, nullable)
            }
        }
    }

    private fun annotations(max: Int = 2): List<AnnotationUse> {
        val annos = refs.filter { it.kind == ClassKind.ANNOTATION }
        if (annos.isEmpty() || !chance(0.4)) return emptyList()
        return List(1 + r.nextInt(max)) {
            val a = pick(annos)
            AnnotationUse(
                a.id,
                args =
                    mapOf(
                        "s" to AnnotationArg.Str("v${r.nextInt(100)}"),
                        "i" to AnnotationArg.IntLit(r.nextInt(-5, 100)),
                        "b" to AnnotationArg.Bool(r.nextBoolean()),
                        "k" to AnnotationArg.ClassLit(refs.filter { it.kind != ClassKind.ANNOTATION }.randomOrNull(r)?.id),
                        "a" to AnnotationArg.StrArray(List(r.nextInt(3)) { "x$it" }),
                    ).filter { chance(0.7) },
                site = pick(UseSite.entries),
            )
        }
    }

    private fun property(tps: List<TypeParamSpec>): PropertyMember =
        PropertyMember(next("p"), type(tps, 0), mutable = chance(0.3), visibility = visibility(), annotations = annotations())

    private fun function(classTps: List<TypeParamSpec>): FunctionMember {
        val ftps = if (chance(0.2)) listOf(TypeParamSpec("R0", bound = if (chance(0.3)) Bound.AnyBound else Bound.None)) else emptyList()
        val all = classTps + ftps
        return FunctionMember(
            name = next("f"),
            typeParams = ftps,
            params = List(r.nextInt(3)) { ParamSpec(next("a"), type(all, 0), hasDefault = chance(0.3), annotations = annotations(1)) },
            returnType = type(all, 0),
            visibility = visibility(),
            annotations = annotations(1),
        )
    }

    private fun cls(
        depth: Int,
        parentKind: ClassKind?,
    ): KClassDecl {
        val id = next("C")
        val kind = classKind()
        val tps = typeParams()
        val ctor =
            List(r.nextInt(3)) {
                ParamSpec(
                    next("c"),
                    if (kind == ClassKind.VALUE) TypeRef.Builtin(pick(listOf(BuiltinType.INT, BuiltinType.STRING, BuiltinType.LONG))) else type(tps, 0),
                    kind = pick(PropKind.entries),
                    visibility = visibility(),
                    hasDefault = chance(0.3),
                    annotations = annotations(1),
                )
            }
        val members =
            List(r.nextInt(shape.maxMembers + 1)) { if (chance(0.5)) property(tps) else function(tps) }
        val nested =
            if (depth < shape.maxNestingDepth && chance(0.35)) {
                List(1 + r.nextInt(2)) { cls(depth + 1, kind) }
            } else {
                emptyList()
            }
        val ifaces = refs.filter { it.kind == ClassKind.INTERFACE && it.typeParams == 0 }
        val hasCompanion = chance(0.15)
        val decl =
            KClassDecl(
                id = id,
                kind = kind,
                visibility = visibility(),
                typeParams = tps,
                ctorParams = ctor,
                members = members,
                nested = nested,
                inner = parentKind != null && chance(0.3),
                annotations = annotations(),
                superInterfaces = if (ifaces.isNotEmpty() && chance(0.4)) List(1 + r.nextInt(2)) { pick(ifaces).id } else emptyList(),
                extendsSealedOuter = parentKind == ClassKind.SEALED && chance(0.8),
                enumEntries = List(1 + r.nextInt(3)) { "E$it" },
                hasCompanion = hasCompanion,
                companion = if (hasCompanion) listOf(property(emptyList())) else emptyList(),
                annotationParams =
                    listOf(
                        AnnotationParamSpec("s", AnnotationParamType.STRING, chance(0.5)),
                        AnnotationParamSpec("i", AnnotationParamType.INT, chance(0.5)),
                        AnnotationParamSpec("b", AnnotationParamType.BOOLEAN, chance(0.5)),
                        AnnotationParamSpec("k", AnnotationParamType.KCLASS, chance(0.5)),
                        AnnotationParamSpec("a", AnnotationParamType.STRING_ARRAY, chance(0.5)),
                    ).filter { chance(0.5) },
            )
        refs += Ref(id, module, kind, if (kind in setOf(ClassKind.CLASS, ClassKind.DATA, ClassKind.INTERFACE)) tps.size else 0)
        return decl
    }
}

/** 生成器の次元ごとの値を、生成したプロジェクトから数える（偏りの確認用。ADR-022 の 3 節）。 */
public fun ProjectSpec.features(): Set<String> {
    val out = HashSet<String>()
    if (modules.size > 1) out += "layout=multi-module"
    for (m in modules) {
        if (m.files.size > 1) out += "layout=multi-file"
        for (f in m.files) {
            when (f) {
                is JavaFileSpec -> out += "java=mixed"
                is KotlinFileSpec -> f.decls.forEach { featuresOf(it, out) }
            }
        }
    }
    return out
}

private fun featuresOf(
    d: KTopLevel,
    out: MutableSet<String>,
) {
    out += "vis=${d.visibility.name.lowercase()}"
    when (d) {
        is KTypeAlias -> out += "decl=typealias"
        is KTopFunction -> {
            out += "decl=function"
            if (d.receiver != null) out += "fn=extension"
            if (d.fn.typeParams.isNotEmpty()) out += "generics=function"
        }
        is KTopProperty -> out += if (d.isConst) "decl=const" else "decl=property"
        is KClassDecl -> classFeatures(d, out, nested = false)
    }
}

private fun classFeatures(
    d: KClassDecl,
    out: MutableSet<String>,
    nested: Boolean,
) {
    out += "decl=${d.kind.name.lowercase()}"
    if (nested) out += if (d.inner) "nesting=inner" else "nesting=nested"
    if (d.typeParams.isNotEmpty()) out += "generics=class"
    d.typeParams.forEach {
        if (it.variance != Variance.INVARIANT) out += "generics=variance-${it.variance.name.lowercase()}"
        if (it.bound != Bound.None) out += "generics=bound"
    }
    if (d.annotations.isNotEmpty()) out += "annotation=class"
    (d.ctorParams.flatMap { it.annotations } + d.members.flatMap { it.annotations })
        .forEach { out += "annotation=site-${it.site.name.lowercase()}" }
    if (d.superInterfaces.isNotEmpty()) out += "misc=inheritance"
    if (d.extendsSealedOuter) out += "misc=sealed-subclass"
    if (d.hasCompanion) out += "misc=companion"
    if (d.ctorParams.any { it.hasDefault }) out += "ctor=default-arg"
    d.members.filterIsInstance<PropertyMember>().forEach { if (it.type.nullable) out += "types=nullable" }
    d.nested.forEach { classFeatures(it, out, nested = true) }
}

private fun <T> List<T>.randomOrNull(r: Random): T? = if (isEmpty()) null else this[r.nextInt(size)]

/** kotest の RandomSource から生成する（テスト用の近道）。 */
public fun RandomSource.projectSpec(shape: ProjectShape = ProjectShape.DEFAULT): ProjectSpec = generateProject(random, shape)

package me.tbsten.analysiscodegen.pbt.source

import io.kotest.property.Shrinker

/**
 * [ProjectSpec] の縮小（ADR-022 の 5 節）。
 *
 * 大きい単位から順に外す: モジュール → 依存 → ファイル → トップレベル宣言 → 入れ子 → メンバー →
 * コンストラクタ引数 → 注釈・型パラメータ・継承・companion → 型を `Int` に。
 * 候補はすべて [normalize] を通すので、縮小の途中でもコンパイルできる形を保つ（PBT-06）。
 */
public object ProjectShrinker : Shrinker<ProjectSpec> {
    override fun shrink(value: ProjectSpec): List<ProjectSpec> {
        val out = LinkedHashSet<ProjectSpec>()
        val ms = value.modules
        // モジュール
        if (ms.size > 1) ms.indices.forEach { i -> out += value.copy(modules = ms.filterIndexed { j, _ -> j != i }) }
        ms.forEachIndexed { i, m ->
            m.dependsOn.forEach { dep -> out += value.replaceModule(i, m.copy(dependsOn = m.dependsOn - dep)) }
        }
        // ファイル
        ms.forEachIndexed { i, m ->
            m.files.indices.forEach { fi -> out += value.replaceModule(i, m.copy(files = m.files.filterIndexed { j, _ -> j != fi })) }
        }
        // トップレベル宣言
        ms.forEachIndexed { i, m ->
            m.files.forEachIndexed { fi, f ->
                when (f) {
                    is KotlinFileSpec ->
                        f.decls.indices.forEach { di ->
                            out += value.replaceFile(i, fi, f.copy(decls = f.decls.filterIndexed { j, _ -> j != di }))
                        }
                    is JavaFileSpec ->
                        f.cls.fields.indices.forEach { k ->
                            out += value.replaceFile(i, fi, f.copy(cls = f.cls.copy(fields = f.cls.fields.filterIndexed { j, _ -> j != k })))
                        }
                }
            }
        }
        // クラスの中身
        for (id in value.classIds()) {
            for (edit in CLASS_EDITS) {
                out += value.mapClass(id, edit)
            }
        }
        // トップレベルの関数・プロパティ・typealias を単純に
        out += value.mapTopLevel { d ->
            when (d) {
                is KTopFunction -> d.copy(fn = d.fn.simplified(), receiver = null)
                is KTopProperty -> d.copy(prop = d.prop.copy(type = TypeRef.INT, annotations = emptyList(), mutable = false), isConst = false)
                is KTypeAlias -> d.copy(target = TypeRef.INT)
                else -> d
            }
        }
        return out.map { normalize(it) }.filter { it != value }.distinct()
    }
}

private val CLASS_EDITS: List<(KClassDecl) -> KClassDecl?> =
    listOf(
        { d -> if (d.nested.isEmpty()) d else d.copy(nested = emptyList()) },
        { d -> d.copy(nested = d.nested.dropLast(1)) },
        { d -> d.copy(members = emptyList()) },
        { d -> d.copy(members = d.members.dropLast(1)) },
        { d -> d.copy(members = d.members.drop(1)) },
        { d -> d.copy(ctorParams = d.ctorParams.dropLast(1)) },
        { d -> d.copy(ctorParams = d.ctorParams.drop(1)) },
        { d ->
            d.copy(
                annotations = emptyList(),
                ctorParams = d.ctorParams.map { it.copy(annotations = emptyList()) },
                members =
                    d.members.map {
                        when (it) {
                            is PropertyMember -> it.copy(annotations = emptyList())
                            is FunctionMember -> it.copy(annotations = emptyList(), params = it.params.map { p -> p.copy(annotations = emptyList()) })
                        }
                    },
            )
        },
        { d -> d.copy(typeParams = emptyList()) },
        { d -> d.copy(superInterfaces = emptyList(), extendsSealedOuter = false) },
        { d -> d.copy(hasCompanion = false) },
        { d -> d.copy(inner = false) },
        { d -> d.copy(annotationParams = d.annotationParams.dropLast(1)) },
        { d -> d.copy(enumEntries = d.enumEntries.take(1)) },
        { d -> if (d.kind == ClassKind.CLASS) d else d.copy(kind = ClassKind.CLASS) },
        { d -> d.copy(visibility = Visibility.PUBLIC) },
        { d ->
            d.copy(
                ctorParams = d.ctorParams.map { it.copy(type = TypeRef.INT, hasDefault = false) },
                members =
                    d.members.map {
                        when (it) {
                            is PropertyMember -> it.copy(type = TypeRef.INT, mutable = false)
                            is FunctionMember -> it.simplified()
                        }
                    },
            )
        },
    )

private fun FunctionMember.simplified() =
    copy(typeParams = emptyList(), params = params.map { it.copy(type = TypeRef.INT, hasDefault = false) }, returnType = TypeRef.INT)

private fun ProjectSpec.replaceModule(
    i: Int,
    m: ModuleSpec,
) = copy(modules = modules.mapIndexed { j, x -> if (j == i) m else x })

private fun ProjectSpec.replaceFile(
    mi: Int,
    fi: Int,
    f: SourceFileSpec,
) = replaceModule(mi, modules[mi].copy(files = modules[mi].files.mapIndexed { j, x -> if (j == fi) f else x }))

/** 全クラス（入れ子を含む）の id。 */
public fun ProjectSpec.classIds(): List<String> {
    val out = mutableListOf<String>()

    fun walk(d: KClassDecl) {
        out += d.id
        d.nested.forEach(::walk)
    }
    modules.forEach { m -> m.files.filterIsInstance<KotlinFileSpec>().forEach { f -> f.decls.filterIsInstance<KClassDecl>().forEach(::walk) } }
    return out
}

/** id のクラスを [f] で置き換える（null を返すと消す）。 */
public fun ProjectSpec.mapClass(
    id: String,
    f: (KClassDecl) -> KClassDecl?,
): ProjectSpec {
    fun walk(d: KClassDecl): KClassDecl? = if (d.id == id) f(d) else d.copy(nested = d.nested.mapNotNull(::walk))
    return mapTopLevelNotNull { if (it is KClassDecl) walk(it) else it }
}

public fun ProjectSpec.mapTopLevel(f: (KTopLevel) -> KTopLevel): ProjectSpec = mapTopLevelNotNull(f)

public fun ProjectSpec.mapTopLevelNotNull(f: (KTopLevel) -> KTopLevel?): ProjectSpec =
    copy(
        modules =
            modules.map { m ->
                m.copy(files = m.files.map { file -> if (file is KotlinFileSpec) file.copy(decls = file.decls.mapNotNull(f)) else file })
            },
    )

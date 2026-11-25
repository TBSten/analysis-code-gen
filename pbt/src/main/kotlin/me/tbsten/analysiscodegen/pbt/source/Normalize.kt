package me.tbsten.analysiscodegen.pbt.source

/**
 * [ProjectSpec] を「必ずコンパイルできる形」に直す。
 *
 * 生成器・縮小・変更の列はどれも、雑に作った候補をこの関数に通してから使う。
 * 規則は 2 段:
 * 1. 構造（宣言の種類ごとの制約・名前の重複・モジュールの依存順）
 * 2. 参照（型・注釈・継承の参照先が存在し、見えて、公開範囲を漏らさず、変位に反しないこと）
 *
 * 直せないものは安全な既定値（`Int`・注釈を外す・継承を外す）に置き換える。
 * `normalize(normalize(x)) == normalize(x)`（冪等）であることを PBT で確かめている。
 */
public fun normalize(spec: ProjectSpec): ProjectSpec {
    val structural = Structural().run(spec)
    return References(structural).run()
}

/** 妥当かどうか（[normalize] の不動点か）。 */
public fun ProjectSpec.isValid(): Boolean = normalize(this) == this

private val SCALARS = setOf(BuiltinType.INT, BuiltinType.LONG, BuiltinType.STRING)

// ---------------------------------------------------------------------------------------------
// 1. 構造
// ---------------------------------------------------------------------------------------------

private class Structural {
    private val seenIds = HashSet<String>()
    private val memberNames = HashSet<String>()
    private val topNames = HashSet<Pair<String, String>>()
    private val fileNames = HashSet<Pair<String, String>>()

    fun run(spec: ProjectSpec): ProjectSpec {
        val moduleNames = HashSet<String>()
        val modules =
            spec.modules.mapNotNull { m ->
                if (!moduleNames.add(m.name)) return@mapNotNull null
                val deps = m.dependsOn.filter { it in moduleNames && it != m.name }.distinct()
                m.copy(dependsOn = deps, files = m.files.mapNotNull { file(it) })
            }
        return ProjectSpec(if (modules.isEmpty()) listOf(ModuleSpec("m0", emptyList(), emptyList())) else modules)
    }

    private fun file(f: SourceFileSpec): SourceFileSpec? =
        when (f) {
            is KotlinFileSpec -> {
                var name = f.name
                if (!fileNames.add(f.pkg to name)) {
                    var i = 0
                    while (!fileNames.add(f.pkg to "${f.name}_$i")) i++
                    name = "${f.name}_$i"
                }
                f.copy(name = name, decls = f.decls.mapNotNull { top(it, f.pkg) })
            }
            is JavaFileSpec -> {
                if (!seenIds.add(f.cls.id)) {
                    null
                } else {
                    val name = uniqueTop(f.pkg, f.cls.name, f.cls.id)
                    val names = HashSet<String>()
                    f.copy(cls = f.cls.copy(name = name, fields = f.cls.fields.filter { names.add(it.name) }))
                }
            }
        }

    private fun uniqueTop(
        pkg: String,
        name: String,
        id: String,
    ): String {
        if (topNames.add(pkg to name)) return name
        if (topNames.add(pkg to id)) return id
        var i = 0
        while (!topNames.add(pkg to "${id}_$i")) i++
        return "${id}_$i"
    }

    private fun uniqueMember(name: String): String {
        if (memberNames.add(name)) return name
        var i = 0
        while (!memberNames.add("${name}_$i")) i++
        return "${name}_$i"
    }

    private fun top(
        d: KTopLevel,
        pkg: String,
    ): KTopLevel? {
        if (!seenIds.add(d.id)) return null
        return when (d) {
            is KClassDecl -> cls(d.copy(name = uniqueTop(pkg, d.name, d.id)), parent = null)
            is KTypeAlias -> d.copy(name = uniqueTop(pkg, d.name, d.id))
            is KTopFunction -> d.copy(fn = fn(d.fn.copy(name = uniqueMember(d.fn.name)), allowedVis = Visibility.entries))
            is KTopProperty -> {
                val p = d.prop.copy(name = uniqueMember(d.prop.name))
                val t = p.type
                val constOk = !p.mutable && t is TypeRef.Builtin && !t.nullable && t.type in setOf(BuiltinType.INT, BuiltinType.STRING)
                d.copy(prop = p, isConst = d.isConst && constOk)
            }
        }
    }

    private fun fn(
        f: FunctionMember,
        allowedVis: List<Visibility>,
    ): FunctionMember {
        val tpNames = HashSet<String>()
        val pNames = HashSet<String>()
        return f.copy(
            visibility = if (f.visibility in allowedVis) f.visibility else Visibility.PUBLIC,
            typeParams =
                f.typeParams
                    .filter { tpNames.add(it.name) }
                    .map { it.copy(variance = Variance.INVARIANT) },
            params = f.params.filter { pNames.add(it.name) }.map { it.copy(kind = PropKind.PLAIN, visibility = Visibility.PUBLIC) },
        )
    }

    private fun cls(
        d0: KClassDecl,
        parent: KClassDecl?,
    ): KClassDecl {
        var d = d0
        val k = d.kind
        // 入れ子の可否
        val innerOk =
            parent != null &&
                parent.kind in setOf(ClassKind.CLASS, ClassKind.DATA) &&
                parent.typeParams.isEmpty() &&
                k == ClassKind.CLASS
        d = d.copy(inner = d.inner && innerOk)
        if (parent?.kind == ClassKind.INTERFACE) d = d.copy(visibility = Visibility.PUBLIC)
        val sealedOk = parent?.kind == ClassKind.SEALED && k in setOf(ClassKind.CLASS, ClassKind.DATA, ClassKind.OBJECT) && !d.inner
        d = d.copy(extendsSealedOuter = d.extendsSealedOuter && sealedOk)

        // 種類ごとの制約
        if (k in setOf(ClassKind.OBJECT, ClassKind.ENUM, ClassKind.SEALED, ClassKind.VALUE, ClassKind.ANNOTATION)) {
            d = d.copy(typeParams = emptyList())
        }
        if (k in setOf(ClassKind.INTERFACE, ClassKind.OBJECT, ClassKind.ENUM, ClassKind.SEALED, ClassKind.ANNOTATION)) {
            d = d.copy(ctorParams = emptyList())
        }
        if (k != ClassKind.ENUM) d = d.copy(enumEntries = emptyList())
        if (k != ClassKind.ANNOTATION) d = d.copy(annotationParams = emptyList())
        if (k == ClassKind.ENUM) {
            val entries = d.enumEntries.distinct().ifEmpty { listOf("A") }
            d = d.copy(enumEntries = entries)
        }
        if (k == ClassKind.DATA) {
            val ps = d.ctorParams.map { if (it.kind == PropKind.PLAIN) it.copy(kind = PropKind.VAL) else it }
            d = d.copy(ctorParams = ps.ifEmpty { listOf(ParamSpec("v${d.id}", TypeRef.INT, PropKind.VAL)) })
        }
        if (k == ClassKind.VALUE) {
            val first = d.ctorParams.firstOrNull()
            val t = first?.type
            val okType = if (t is TypeRef.Builtin && t.type in SCALARS && !t.nullable) t else TypeRef.INT
            d =
                d.copy(
                    ctorParams =
                        listOf(
                            ParamSpec(first?.name ?: "v${d.id}", okType, PropKind.VAL, Visibility.PUBLIC, false, emptyList()),
                        ),
                    nested = emptyList(),
                    inner = false,
                    members = d.members.filterIsInstance<FunctionMember>(),
                )
        }
        if (k == ClassKind.ANNOTATION) {
            val names = HashSet<String>()
            d =
                d.copy(
                    members = emptyList(),
                    nested = emptyList(),
                    superInterfaces = emptyList(),
                    hasCompanion = false,
                    annotationParams = d.annotationParams.filter { names.add(it.name) },
                )
        }
        if (k == ClassKind.OBJECT || d.inner) d = d.copy(hasCompanion = false)
        if (d.inner) d = d.copy(nested = emptyList())
        if (!d.hasCompanion) d = d.copy(companion = emptyList())

        // 名前
        val tpNames = HashSet<String>()
        d = d.copy(typeParams = d.typeParams.filter { tpNames.add(it.name) })
        val variantOk = { tp: TypeParamSpec -> if (tp.variance != Variance.INVARIANT && tp.bound !in setOf(Bound.None, Bound.AnyBound)) tp.copy(bound = Bound.None) else tp }
        d = d.copy(typeParams = d.typeParams.map(variantOk))
        val memberVis =
            if (k == ClassKind.INTERFACE) listOf(Visibility.PUBLIC) else Visibility.entries
        d =
            d.copy(
                ctorParams =
                    d.ctorParams.map {
                        val name = if (it.kind == PropKind.PLAIN) it.name else uniqueMember(it.name)
                        it.copy(name = name, visibility = if (it.kind == PropKind.PLAIN) Visibility.PUBLIC else it.visibility)
                    }.distinctBy { it.name },
                members = d.members.map { member(it, memberVis) },
                companion = d.companion.map { member(it, if (k == ClassKind.INTERFACE) listOf(Visibility.PUBLIC) else Visibility.entries) },
                superInterfaces = d.superInterfaces.distinct().filter { it != d.id },
            )
        val nestedNames = HashSet<String>()
        val nested =
            d.nested.mapNotNull { n ->
                if (!seenIds.add(n.id)) return@mapNotNull null
                val name = if (nestedNames.add(n.name)) n.name else n.id.also { nestedNames.add(it) }
                cls(n.copy(name = name), parent = d)
            }
        return d.copy(nested = nested)
    }

    private fun member(
        m: Member,
        allowedVis: List<Visibility>,
    ): Member =
        when (m) {
            is PropertyMember ->
                m.copy(
                    name = uniqueMember(m.name),
                    visibility = if (m.visibility in allowedVis) m.visibility else Visibility.PUBLIC,
                )
            is FunctionMember -> fn(m.copy(name = uniqueMember(m.name)), allowedVis)
        }
}

// ---------------------------------------------------------------------------------------------
// 2. 参照
// ---------------------------------------------------------------------------------------------

/** 宣言の索引。 */
internal class DeclInfo(
    val id: String,
    val module: String,
    val fileKey: String,
    val pkg: String,
    /** パッケージからの名前の列（入れ子なら外側から） */
    val path: List<String>,
    val kind: ClassKind?,
    val isAlias: Boolean,
    val effective: Visibility,
    val typeParamCount: Int,
    val topLevel: Boolean,
    val annotationParams: List<AnnotationParamSpec>,
) {
    val fqName: String get() = (listOf(pkg) + path).joinToString(".")
}

internal class JavaInfo(val id: String, val module: String, val pkg: String, val name: String) {
    val fqName: String get() = "$pkg.$name"
}

internal class SpecIndex(spec: ProjectSpec) {
    val decls = LinkedHashMap<String, DeclInfo>()
    val javas = LinkedHashMap<String, JavaInfo>()

    /** モジュール → 推移的に依存するモジュール（自分を含む） */
    val visibleModules = HashMap<String, Set<String>>()

    init {
        for (m in spec.modules) {
            visibleModules[m.name] = setOf(m.name) + m.dependsOn.flatMap { visibleModules[it].orEmpty() }
            for (f in m.files) {
                when (f) {
                    is KotlinFileSpec -> {
                        val key = "${m.name}/${f.pkg}/${f.name}"
                        for (d in f.decls) {
                            when (d) {
                                is KClassDecl -> addClass(d, m.name, key, f.pkg, emptyList(), Visibility.PUBLIC, true)
                                is KTypeAlias ->
                                    decls[d.id] =
                                        DeclInfo(d.id, m.name, key, f.pkg, listOf(d.name), null, true, d.visibility, 0, true, emptyList())
                                else -> Unit
                            }
                        }
                    }
                    is JavaFileSpec -> javas[f.cls.id] = JavaInfo(f.cls.id, m.name, f.pkg, f.cls.name)
                }
            }
        }
    }

    private fun addClass(
        d: KClassDecl,
        module: String,
        key: String,
        pkg: String,
        outer: List<String>,
        outerEff: Visibility,
        top: Boolean,
    ) {
        val path = outer + d.name
        val eff = outerEff.min(d.visibility)
        decls[d.id] = DeclInfo(d.id, module, key, pkg, path, d.kind, false, eff, d.typeParams.size, top, d.annotationParams)
        d.nested.forEach { addClass(it, module, key, pkg, path, eff, false) }
    }
}

private enum class Pos { OUT, IN, INV }

private class Ctx(
    val module: String,
    val typeParams: Map<String, Variance>,
    /** この位置の公開範囲。参照先はこれ以上に広くなければならない */
    val vis: Visibility,
    val inAlias: Boolean = false,
) {
    fun with(
        tps: List<TypeParamSpec>,
        vis: Visibility = this.vis,
    ) = Ctx(module, typeParams + tps.associate { it.name to it.variance }, vis, inAlias)

    fun vis(v: Visibility) = Ctx(module, typeParams, v, inAlias)
}

private class References(private val spec: ProjectSpec) {
    private val index = SpecIndex(spec)

    fun run(): ProjectSpec {
        val ifaceEdges = HashMap<String, MutableSet<String>>()
        return ProjectSpec(
            spec.modules.map { m ->
                m.copy(
                    files =
                        m.files.map { f ->
                            when (f) {
                                is KotlinFileSpec -> f.copy(decls = f.decls.map { top(it, m.name, ifaceEdges) })
                                is JavaFileSpec -> f.copy(cls = java(f.cls, m.name))
                            }
                        },
                )
            },
        )
    }

    private fun accessible(
        info: DeclInfo,
        ctx: Ctx,
    ): Boolean {
        if (info.effective == Visibility.PRIVATE) return false
        if (info.module !in index.visibleModules[ctx.module].orEmpty()) return false
        if (info.module != ctx.module && info.effective != Visibility.PUBLIC) return false
        return info.effective.ordinal >= ctx.vis.ordinal
    }

    private fun type(
        t: TypeRef,
        ctx: Ctx,
        pos: Pos,
        nested: Boolean = false,
    ): TypeRef =
        when (t) {
            is TypeRef.Builtin -> {
                val args = (0 until t.type.arity).map { i -> type(t.args.getOrElse(i) { TypeRef.INT }, ctx, Pos.INV, nested = true) }
                t.copy(args = args)
            }
            is TypeRef.Decl -> {
                val info = index.decls[t.id]
                val ok =
                    info != null &&
                        accessible(info, ctx) &&
                        info.kind != ClassKind.ANNOTATION &&
                        !(ctx.inAlias && info.isAlias)
                if (ok) t.copy(starArgs = info!!.typeParamCount) else TypeRef.Builtin(BuiltinType.INT, nullable = t.nullable)
            }
            is TypeRef.Java -> {
                val info = index.javas[t.id]
                if (info != null && info.module in index.visibleModules[ctx.module].orEmpty()) {
                    t.copy(nullable = true)
                } else {
                    TypeRef.Builtin(BuiltinType.INT, nullable = t.nullable)
                }
            }
            is TypeRef.Param -> {
                val v = ctx.typeParams[t.name]
                val ok =
                    when (v) {
                        null -> false
                        Variance.INVARIANT -> true
                        Variance.OUT -> pos == Pos.OUT && !nested
                        Variance.IN -> pos == Pos.IN && !nested
                    }
                if (ok) t else TypeRef.Builtin(BuiltinType.INT, nullable = t.nullable)
            }
        }

    private fun annotations(
        uses: List<AnnotationUse>,
        ctx: Ctx,
        sites: Set<UseSite>,
    ): List<AnnotationUse> {
        // 注釈は公開範囲を漏らさないので、見えるかどうかだけを確かめる
        val c = ctx.vis(Visibility.PRIVATE)
        val seen = HashSet<String>()
        return uses.mapNotNull { u ->
            val info = index.decls[u.annotationId] ?: return@mapNotNull null
            if (info.kind != ClassKind.ANNOTATION || !accessible(info, c) || !seen.add(u.annotationId)) return@mapNotNull null
            val args =
                info.annotationParams.mapNotNull { p ->
                    val a = u.args[p.name]
                    val fixed = a?.let { arg(it, p.type, c) }
                    when {
                        fixed != null -> p.name to fixed
                        p.hasDefault -> null
                        else -> p.name to defaultArg(p.type)
                    }
                }.toMap()
            u.copy(args = args, site = if (u.site in sites) u.site else UseSite.NONE)
        }
    }

    private fun arg(
        a: AnnotationArg,
        t: AnnotationParamType,
        ctx: Ctx,
    ): AnnotationArg? =
        when (t) {
            AnnotationParamType.STRING -> (a as? AnnotationArg.Str)?.let { AnnotationArg.Str(it.value.filter { c -> c.isLetterOrDigit() }) }
            AnnotationParamType.INT -> a as? AnnotationArg.IntLit
            AnnotationParamType.BOOLEAN -> a as? AnnotationArg.Bool
            AnnotationParamType.STRING_ARRAY ->
                (a as? AnnotationArg.StrArray)?.let { AnnotationArg.StrArray(it.values.map { v -> v.filter { c -> c.isLetterOrDigit() } }) }
            AnnotationParamType.KCLASS ->
                (a as? AnnotationArg.ClassLit)?.let { lit ->
                    val info = lit.id?.let { index.decls[it] }
                    if (info != null && !info.isAlias && accessible(info, ctx)) lit else AnnotationArg.ClassLit(null)
                }
        }

    private fun bound(
        b: Bound,
        ctx: Ctx,
    ): Bound =
        when (b) {
            is Bound.Interface -> {
                val info = index.decls[b.id]
                if (info != null && info.kind == ClassKind.INTERFACE && info.typeParamCount == 0 && accessible(info, ctx)) b else Bound.None
            }
            else -> b
        }

    private fun typeParams(
        tps: List<TypeParamSpec>,
        ctx: Ctx,
    ) = tps.map { it.copy(bound = bound(it.bound, ctx)) }

    private fun top(
        d: KTopLevel,
        module: String,
        edges: HashMap<String, MutableSet<String>>,
    ): KTopLevel {
        val base = Ctx(module, emptyMap(), d.visibility)
        return when (d) {
            is KClassDecl -> cls(d, base, Visibility.PUBLIC, edges)
            is KTypeAlias -> d.copy(target = type(d.target, Ctx(module, emptyMap(), d.visibility, inAlias = true), Pos.INV))
            is KTopFunction -> {
                val fn = function(d.fn, base, Visibility.PUBLIC)
                val c = base.with(fn.typeParams, fn.visibility)
                d.copy(fn = fn, receiver = d.receiver?.let { type(it, c, Pos.IN) })
            }
            is KTopProperty -> {
                val p = d.prop
                val t = type(p.type, base.vis(p.visibility), if (p.mutable) Pos.INV else Pos.OUT)
                val constOk = !p.mutable && t is TypeRef.Builtin && !t.nullable && t.type in setOf(BuiltinType.INT, BuiltinType.STRING)
                d.copy(
                    prop = p.copy(type = t, annotations = annotations(p.annotations, base, setOf(UseSite.NONE, UseSite.FIELD, UseSite.GET, UseSite.PROPERTY))),
                    isConst = d.isConst && constOk,
                )
            }
        }
    }

    private fun reaches(
        edges: Map<String, Set<String>>,
        from: String,
        to: String,
    ): Boolean {
        val seen = HashSet<String>()
        val stack = ArrayDeque(listOf(from))
        while (stack.isNotEmpty()) {
            val x = stack.removeLast()
            if (x == to) return true
            if (seen.add(x)) stack.addAll(edges[x].orEmpty())
        }
        return false
    }

    private fun cls(
        d: KClassDecl,
        outerCtx: Ctx,
        outerEff: Visibility,
        edges: HashMap<String, MutableSet<String>>,
    ): KClassDecl {
        val eff = outerEff.min(d.visibility)
        val clsCtx = Ctx(outerCtx.module, emptyMap(), eff)
        // 自分の中の宣言を継承・上限に使うと「supertypes の循環」になる
        val own = descendants(d)
        val tps = typeParams(d.typeParams, clsCtx).map { if (it.bound is Bound.Interface && (it.bound as Bound.Interface).id in own) it.copy(bound = Bound.None) else it }
        val inner = clsCtx.with(tps)

        val supers =
            d.superInterfaces.filter { sid ->
                val info = index.decls[sid]
                val ok =
                    info != null &&
                        sid !in own &&
                        info.kind == ClassKind.INTERFACE &&
                        info.typeParamCount == 0 &&
                        accessible(info, clsCtx) &&
                        !reaches(edges, sid, d.id)
                if (ok) edges.getOrPut(d.id) { mutableSetOf() }.add(sid)
                ok
            }

        val getterOnly = d.kind == ClassKind.INTERFACE || d.kind == ClassKind.VALUE
        val propSites =
            if (getterOnly) {
                setOf(UseSite.NONE, UseSite.GET, UseSite.PROPERTY)
            } else {
                setOf(UseSite.NONE, UseSite.FIELD, UseSite.GET, UseSite.PROPERTY)
            }
        val ctorParams =
            d.ctorParams.map { p ->
                val pos =
                    when (p.kind) {
                        PropKind.VAL -> Pos.OUT
                        PropKind.VAR -> Pos.INV
                        PropKind.PLAIN -> Pos.IN
                    }
                val sites =
                    when {
                        d.kind == ClassKind.VALUE -> setOf(UseSite.NONE)
                        p.kind == PropKind.PLAIN -> setOf(UseSite.NONE, UseSite.PARAM)
                        else -> setOf(UseSite.NONE, UseSite.FIELD, UseSite.GET, UseSite.PARAM, UseSite.PROPERTY)
                    }
                val t = if (d.kind == ClassKind.VALUE) p.type else type(p.type, inner, pos)
                p.copy(type = t, annotations = annotations(p.annotations, inner, sites))
            }
        val members = d.members.map { member(it, inner, eff, propSites) }
        val companion = d.companion.map { member(it, clsCtx, eff, setOf(UseSite.NONE, UseSite.FIELD, UseSite.GET, UseSite.PROPERTY)) }
        val nested = d.nested.map { cls(it, clsCtx, eff, edges) }
        return d.copy(
            typeParams = tps,
            ctorParams = ctorParams,
            members = members,
            companion = companion,
            nested = nested,
            superInterfaces = supers,
            annotations = annotations(d.annotations, clsCtx, setOf(UseSite.NONE)),
        )
    }

    private fun member(
        m: Member,
        ctx: Ctx,
        classEff: Visibility,
        propSites: Set<UseSite>,
    ): Member =
        when (m) {
            is PropertyMember -> {
                val c = ctx.vis(classEff.min(m.visibility))
                m.copy(
                    type = type(m.type, c, if (m.mutable) Pos.INV else Pos.OUT),
                    annotations = annotations(m.annotations, c, propSites),
                )
            }
            is FunctionMember -> function(m, ctx, classEff)
        }

    private fun function(
        f: FunctionMember,
        ctx: Ctx,
        classEff: Visibility,
    ): FunctionMember {
        val v = classEff.min(f.visibility)
        val base = ctx.vis(v)
        val tps = typeParams(f.typeParams, base)
        val c = base.with(tps)
        return f.copy(
            typeParams = tps,
            params = f.params.map { p -> p.copy(type = type(p.type, c, Pos.IN), annotations = annotations(p.annotations, c, setOf(UseSite.NONE))) },
            returnType = type(f.returnType, c, Pos.OUT),
            annotations = annotations(f.annotations, c, setOf(UseSite.NONE)),
        )
    }

    private fun java(
        c: JClassDecl,
        module: String,
    ): JClassDecl {
        val ctx = Ctx(module, emptyMap(), Visibility.PUBLIC)
        return c.copy(
            fields =
                c.fields.map { f ->
                    val t =
                        when (val t0 = f.type) {
                            is TypeRef.Builtin -> if (t0.type.java != null) t0.copy(nullable = false) else TypeRef.INT
                            is TypeRef.Decl -> {
                                val info = index.decls[t0.id]
                                val ok =
                                    info != null &&
                                        info.topLevel &&
                                        !info.isAlias &&
                                        info.typeParamCount == 0 &&
                                        info.kind in JAVA_REFERABLE &&
                                        accessible(info, ctx)
                                if (ok) t0.copy(nullable = false, starArgs = 0) else TypeRef.INT
                            }
                            is TypeRef.Java ->
                                if (index.javas[t0.id]?.module in index.visibleModules[module].orEmpty()) t0.copy(nullable = true) else TypeRef.INT
                            is TypeRef.Param -> TypeRef.INT
                        }
                    f.copy(type = t)
                },
        )
    }
}

private fun descendants(d: KClassDecl): Set<String> {
    val out = HashSet<String>()

    fun walk(x: KClassDecl) {
        out += x.id
        x.nested.forEach(::walk)
    }
    walk(d)
    return out
}

private val JAVA_REFERABLE = setOf(ClassKind.CLASS, ClassKind.DATA, ClassKind.INTERFACE, ClassKind.ENUM, ClassKind.OBJECT, ClassKind.SEALED)

internal fun defaultArg(t: AnnotationParamType): AnnotationArg =
    when (t) {
        AnnotationParamType.STRING -> AnnotationArg.Str("d")
        AnnotationParamType.INT -> AnnotationArg.IntLit(0)
        AnnotationParamType.BOOLEAN -> AnnotationArg.Bool(false)
        AnnotationParamType.KCLASS -> AnnotationArg.ClassLit(null)
        AnnotationParamType.STRING_ARRAY -> AnnotationArg.StrArray(emptyList())
    }

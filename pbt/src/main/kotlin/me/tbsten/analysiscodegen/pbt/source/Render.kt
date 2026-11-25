package me.tbsten.analysiscodegen.pbt.source

/** 書き出したプロジェクト。パスは `<モジュール>/src/main/(kotlin|java)/<パッケージ>/<ファイル>`。 */
public data class RenderedProject(
    val modules: List<RenderedModule>,
) {
    /** 全ファイル（パス → 中身） */
    val files: Map<String, String> get() = modules.flatMap { m -> m.files.map { (p, c) -> "${m.name}/$p" to c } }.toMap()

    public fun describe(): String =
        buildString {
            appendLine("ProjectSpec(")
            for (m in modules) {
                appendLine("  module ${m.name} dependsOn=${m.dependsOn}")
                for ((path, content) in m.files) {
                    appendLine("  // FILE: ${m.name}/$path")
                    content.lineSequence().filter { it.isNotBlank() }.forEach { appendLine("  $it") }
                }
            }
            append(")")
        }
}

public data class RenderedModule(
    val name: String,
    val dependsOn: List<String>,
    /** モジュール内の相対パス → 中身 */
    val files: Map<String, String>,
)

/** [ProjectSpec] を Kotlin / Java のソースに書き出す。型はすべて完全修飾名で書く（import を使わない）。 */
public fun render(spec: ProjectSpec): RenderedProject {
    val r = Renderer(SpecIndex(spec))
    return RenderedProject(
        spec.modules.map { m ->
            RenderedModule(
                m.name,
                m.dependsOn,
                m.files.associate { f ->
                    val dir = f.pkg.replace('.', '/')
                    when (f) {
                        is KotlinFileSpec -> "src/main/kotlin/$dir/${f.name}.kt" to r.kotlinFile(f)
                        is JavaFileSpec -> "src/main/java/$dir/${f.cls.name}.java" to r.javaFile(f)
                    }
                },
            )
        },
    )
}

private class Renderer(private val index: SpecIndex) {
    fun kotlinFile(f: KotlinFileSpec): String =
        buildString {
            appendLine("package ${f.pkg}")
            appendLine()
            for (d in f.decls) {
                top(d, this)
                appendLine()
            }
        }

    fun javaFile(f: JavaFileSpec): String =
        buildString {
            appendLine("package ${f.pkg};")
            appendLine()
            appendLine("public class ${f.cls.name} {")
            for (fl in f.cls.fields) appendLine("    public ${javaType(fl.type)} ${fl.name};")
            appendLine("}")
        }

    private fun javaType(t: TypeRef): String =
        when (t) {
            is TypeRef.Builtin -> t.type.java ?: "int"
            is TypeRef.Decl -> index.decls.getValue(t.id).fqName
            is TypeRef.Java -> index.javas.getValue(t.id).fqName
            is TypeRef.Param -> "int"
        }

    fun type(t: TypeRef): String {
        val base =
            when (t) {
                is TypeRef.Builtin -> if (t.args.isEmpty()) t.type.kotlin else "${t.type.kotlin}<${t.args.joinToString(", ") { type(it) }}>"
                is TypeRef.Decl -> {
                    val info = index.decls.getValue(t.id)
                    if (t.starArgs == 0) info.fqName else "${info.fqName}<${List(t.starArgs) { "*" }.joinToString(", ")}>"
                }
                is TypeRef.Java -> index.javas.getValue(t.id).fqName
                is TypeRef.Param -> t.name
            }
        return if (t.nullable) "$base?" else base
    }

    private fun defaultExpr(t: TypeRef): String {
        if (t.nullable) return "null"
        return when (t) {
            is TypeRef.Builtin ->
                when (t.type) {
                    BuiltinType.INT -> "0"
                    BuiltinType.LONG -> "0L"
                    BuiltinType.BOOLEAN -> "false"
                    BuiltinType.DOUBLE -> "0.0"
                    BuiltinType.STRING -> "\"\""
                    BuiltinType.LIST -> "emptyList()"
                    BuiltinType.SET -> "emptySet()"
                    BuiltinType.MAP -> "emptyMap()"
                }
            else -> "TODO()"
        }
    }

    private fun annotation(u: AnnotationUse): String {
        val info = index.decls.getValue(u.annotationId)
        val args =
            u.args.entries.joinToString(", ") { (k, v) -> "$k = ${annotationArg(v)}" }
        return "@${u.site.prefix}${info.fqName}" + if (args.isEmpty()) "" else "($args)"
    }

    private fun annotationArg(a: AnnotationArg): String =
        when (a) {
            is AnnotationArg.Str -> "\"${a.value}\""
            is AnnotationArg.IntLit -> a.value.toString()
            is AnnotationArg.Bool -> a.value.toString()
            is AnnotationArg.ClassLit -> (a.id?.let { index.decls.getValue(it).fqName } ?: "String") + "::class"
            is AnnotationArg.StrArray -> a.values.joinToString(", ", "[", "]") { "\"$it\"" }
        }

    private fun annos(
        list: List<AnnotationUse>,
        sep: String = " ",
    ) = list.joinToString("") { annotation(it) + sep }

    private fun typeParams(tps: List<TypeParamSpec>): String {
        if (tps.isEmpty()) return ""
        return tps.joinToString(", ", "<", ">") { tp ->
            val b =
                when (val bd = tp.bound) {
                    Bound.None -> ""
                    Bound.AnyBound -> " : Any"
                    Bound.ComparableSelf -> " : Comparable<${tp.name}>"
                    is Bound.Interface -> " : ${index.decls.getValue(bd.id).fqName}"
                }
            "${tp.variance.keyword}${tp.name}$b"
        }
    }

    private fun top(
        d: KTopLevel,
        out: StringBuilder,
    ) {
        when (d) {
            is KClassDecl -> cls(d, out, "", null)
            is KTypeAlias -> out.appendLine("${d.visibility.keyword} typealias ${d.name} = ${type(d.target)}")
            is KTopFunction -> {
                val recv = d.receiver?.let { type(it) + "." } ?: ""
                out.appendLine(function(d.fn, recv))
            }
            is KTopProperty -> {
                val p = d.prop
                val init =
                    if (d.isConst) {
                        if ((p.type as TypeRef.Builtin).type == BuiltinType.INT) "1" else "\"c\""
                    } else {
                        defaultExpr(p.type)
                    }
                val kw = if (d.isConst) "const val" else if (p.mutable) "var" else "val"
                out.appendLine("${annos(p.annotations)}${p.visibility.keyword} $kw ${p.name}: ${type(p.type)} = $init")
            }
        }
    }

    private fun function(
        f: FunctionMember,
        receiver: String = "",
    ): String {
        val tps = typeParams(f.typeParams).let { if (it.isEmpty()) "" else "$it " }
        val params =
            f.params.joinToString(", ") { p ->
                annos(p.annotations) + "${p.name}: ${type(p.type)}" + if (p.hasDefault) " = ${defaultExpr(p.type)}" else ""
            }
        return "${annos(f.annotations)}${f.visibility.keyword} fun $tps$receiver${f.name}($params): ${type(f.returnType)} = TODO()"
    }

    private fun property(
        p: PropertyMember,
        getterOnly: Boolean,
    ): String {
        val kw = if (p.mutable) "var" else "val"
        val head = "${annos(p.annotations)}${p.visibility.keyword} $kw ${p.name}: ${type(p.type)}"
        return if (getterOnly) {
            "$head get() = ${defaultExpr(p.type)}" + if (p.mutable) "\n        set(value) {}" else ""
        } else {
            "$head = ${defaultExpr(p.type)}"
        }
    }

    private fun cls(
        d: KClassDecl,
        out: StringBuilder,
        indent: String,
        outer: KClassDecl?,
    ) {
        val i2 = "$indent    "
        val head = StringBuilder()
        head.append(indent)
        d.annotations.forEach { head.append(annotation(it)).append(' ') }
        if (d.kind == ClassKind.VALUE) head.append("@JvmInline ")
        head.append(d.visibility.keyword).append(' ')
        if (d.inner) head.append("inner ")
        head.append(
            when (d.kind) {
                ClassKind.CLASS -> "class"
                ClassKind.DATA -> "data class"
                ClassKind.INTERFACE -> "interface"
                ClassKind.OBJECT -> "object"
                ClassKind.ENUM -> "enum class"
                ClassKind.SEALED -> "sealed class"
                ClassKind.VALUE -> "value class"
                ClassKind.ANNOTATION -> "annotation class"
            },
        )
        head.append(' ').append(d.name).append(typeParams(d.typeParams))
        if (d.kind == ClassKind.ANNOTATION) {
            if (d.annotationParams.isNotEmpty()) {
                head.append(
                    d.annotationParams.joinToString(", ", "(", ")") { p ->
                        "val ${p.name}: ${p.type.kotlin}" + if (p.hasDefault) " = ${annotationArg(defaultArg(p.type))}" else ""
                    },
                )
            }
        } else if (d.ctorParams.isNotEmpty() || d.kind in setOf(ClassKind.CLASS, ClassKind.DATA, ClassKind.VALUE)) {
            if (d.kind !in setOf(ClassKind.INTERFACE, ClassKind.OBJECT, ClassKind.ENUM, ClassKind.SEALED)) {
                head.append(
                    d.ctorParams.joinToString(", ", "(", ")") { p ->
                        val kw = p.kind.keyword?.let { "${p.visibility.keyword} $it " } ?: ""
                        annos(p.annotations) + kw + "${p.name}: ${type(p.type)}" + if (p.hasDefault) " = ${defaultExpr(p.type)}" else ""
                    },
                )
            }
        }
        val supers = mutableListOf<String>()
        if (d.extendsSealedOuter && outer != null) supers += "${index.decls.getValue(outer.id).fqName}()"
        d.superInterfaces.forEach { supers += index.decls.getValue(it).fqName }
        if (supers.isNotEmpty()) head.append(" : ").append(supers.joinToString(", "))
        out.append(head).appendLine(" {")
        if (d.kind == ClassKind.ENUM) out.append(i2).append(d.enumEntries.joinToString(", ")).appendLine(";")
        val getterOnly = d.kind == ClassKind.INTERFACE || d.kind == ClassKind.VALUE
        for (m in d.members) {
            out.append(i2)
            out.appendLine(
                when (m) {
                    is PropertyMember -> property(m, getterOnly)
                    is FunctionMember -> function(m)
                },
            )
        }
        if (d.hasCompanion) {
            out.append(i2).appendLine("companion object {")
            for (m in d.companion) {
                out.append("$i2    ")
                out.appendLine(
                    when (m) {
                        is PropertyMember -> property(m, getterOnly = false)
                        is FunctionMember -> function(m)
                    },
                )
            }
            out.append(i2).appendLine("}")
        }
        for (n in d.nested) cls(n, out, i2, d)
        out.append(indent).appendLine("}")
    }
}

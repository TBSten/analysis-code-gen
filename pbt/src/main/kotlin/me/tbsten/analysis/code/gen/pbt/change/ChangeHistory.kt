package me.tbsten.analysis.code.gen.pbt.change

import io.kotest.property.Arb
import io.kotest.property.Shrinker
import io.kotest.property.arbitrary.arbitrary
import me.tbsten.analysis.code.gen.pbt.processor.OptionAssignment
import me.tbsten.analysis.code.gen.pbt.processor.OptionTable
import me.tbsten.analysis.code.gen.pbt.processor.ProcessorId
import me.tbsten.analysis.code.gen.pbt.processor.ProcessorSet
import me.tbsten.analysis.code.gen.pbt.source.AnnotationUse
import me.tbsten.analysis.code.gen.pbt.source.ClassKind
import me.tbsten.analysis.code.gen.pbt.source.FunctionMember
import me.tbsten.analysis.code.gen.pbt.source.JavaFileSpec
import me.tbsten.analysis.code.gen.pbt.source.KClassDecl
import me.tbsten.analysis.code.gen.pbt.source.KotlinFileSpec
import me.tbsten.analysis.code.gen.pbt.source.ParamSpec
import me.tbsten.analysis.code.gen.pbt.source.ProjectShape
import me.tbsten.analysis.code.gen.pbt.source.ProjectShrinker
import me.tbsten.analysis.code.gen.pbt.source.ProjectSpec
import me.tbsten.analysis.code.gen.pbt.source.PropKind
import me.tbsten.analysis.code.gen.pbt.source.PropertyMember
import me.tbsten.analysis.code.gen.pbt.source.TypeRef
import me.tbsten.analysis.code.gen.pbt.source.classIds
import me.tbsten.analysis.code.gen.pbt.source.generateProject
import me.tbsten.analysis.code.gen.pbt.source.mapClass
import me.tbsten.analysis.code.gen.pbt.source.mapTopLevelNotNull
import me.tbsten.analysis.code.gen.pbt.source.normalize
import kotlin.random.Random

/**
 * 増分ビルドの入力の 1 ステップ（設計 2.3 節）。
 *
 * 対象は id ではなく「選び方の数」（[pick]）で持ち、適用時にその時点の候補数で割った余りで決める。
 * こうすると、縮小で前の操作や宣言が消えても、どの操作もそのまま適用できる（状態機械 PBT の縮小が壊れない）。
 * 適用後は必ず [normalize] を通すので、どの状態もコンパイルできる。
 */
public sealed interface ChangeOp {
    public val pick: Int

    /** クラスを 1 つ足す（[seed] で中身を決める） */
    public data class AddClass(override val pick: Int, val seed: Long) : ChangeOp

    /** 宣言を消す */
    public data class RemoveDecl(override val pick: Int) : ChangeOp

    /** 名前を変える */
    public data class Rename(override val pick: Int, val suffix: Int) : ChangeOp

    /** プロパティの型の nullable を反転する */
    public data class ToggleNullable(override val pick: Int) : ChangeOp

    /** プロパティの型を変える（String ↔ Int など） */
    public data class ChangeType(override val pick: Int) : ChangeOp

    /** 関数本体だけを変える（ABI は変わらない） */
    public data class ChangeBody(override val pick: Int) : ChangeOp

    /** 注釈を付ける / 外す */
    public data class ToggleAnnotation(override val pick: Int, val annotationPick: Int) : ChangeOp

    /** ファイルを別パッケージへ移す */
    public data class MoveFile(override val pick: Int, val toPackage: Int) : ChangeOp

    /** モジュール間の依存を足す / 外す */
    public data class ToggleDependency(override val pick: Int, val target: Int) : ChangeOp

    /** オプションを切り替える */
    public data class ChangeOption(override val pick: Int, val valuePick: Int) : ChangeOp

    /** Processor を 1 つ足す / 外す */
    public data class ToggleProcessor(override val pick: Int) : ChangeOp
}

/** 増分ビルドの 1 状態。 */
public data class BuildState(
    val project: ProjectSpec,
    val processors: ProcessorSet,
    val options: OptionAssignment,
)

/** 最初の状態と変更の列。[states] は最初の状態を含めて ops.size + 1 個。 */
public data class ChangeHistory(
    val initial: BuildState,
    val ops: List<ChangeOp>,
) {
    public fun states(): List<BuildState> = ops.runningFold(initial) { s, op -> apply(s, op) }

    override fun toString(): String = "ChangeHistory(ops=$ops, initial=${initial.processors} ${initial.options}\n${initial.project})"
}

/** 生成器の版（ADR-022 の 4 節）。 */
public const val CHANGE_GENERATOR_VERSION: Int = 1

/** 変更の列の Arb。最初のプロジェクトは小さめ（[ProjectShape.SMALL]）、変更は 1〜[maxOps] 個。 */
public fun Arb.Companion.changeHistory(
    processors: ProcessorSet = ProcessorSet(listOf(ProcessorId.KOI, ProcessorId.CRM)),
    maxOps: Int = 8,
    shape: ProjectShape = ProjectShape.SMALL,
): Arb<ChangeHistory> =
    arbitrary(shrinker = ChangeHistoryShrinker) { rs ->
        val r = rs.random
        val initial = BuildState(generateProject(r, shape), processors, OptionAssignment(emptyMap()))
        ChangeHistory(initial, List(1 + r.nextInt(maxOps)) { randomOp(r) })
    }

private fun randomOp(r: Random): ChangeOp {
    val p = r.nextInt(1 shl 16)
    return when (r.nextInt(11)) {
        0 -> ChangeOp.AddClass(p, r.nextLong())
        1 -> ChangeOp.RemoveDecl(p)
        2 -> ChangeOp.Rename(p, r.nextInt(100))
        3 -> ChangeOp.ToggleNullable(p)
        4 -> ChangeOp.ChangeType(p)
        5 -> ChangeOp.ChangeBody(p)
        6 -> ChangeOp.ToggleAnnotation(p, r.nextInt(1 shl 16))
        7 -> ChangeOp.MoveFile(p, r.nextInt(3))
        8 -> ChangeOp.ToggleDependency(p, r.nextInt(1 shl 16))
        9 -> ChangeOp.ChangeOption(p, r.nextInt(1 shl 16))
        else -> ChangeOp.ToggleProcessor(p)
    }
}

public object ChangeHistoryShrinker : Shrinker<ChangeHistory> {
    override fun shrink(value: ChangeHistory): List<ChangeHistory> {
        val out = mutableListOf<ChangeHistory>()
        // 変更を後ろから 1 つずつ外す → 前半だけにする → 最初のプロジェクトを縮める
        if (value.ops.size > 1) {
            out += value.copy(ops = value.ops.take(value.ops.size / 2))
            value.ops.indices
                .reversed()
                .forEach { i -> out += value.copy(ops = value.ops.filterIndexed { j, _ -> j != i }) }
        }
        ProjectShrinker.shrink(value.initial.project).take(20).forEach { p -> out += value.copy(initial = value.initial.copy(project = p)) }
        return out
    }
}

/** 1 ステップを適用する。結果は常に正規化済み。 */
public fun apply(
    s: BuildState,
    op: ChangeOp,
): BuildState =
    when (op) {
        is ChangeOp.ChangeOption -> {
            val specs = s.processors.order.flatMap { OptionTable.of(it) }
            if (specs.isEmpty()) {
                s
            } else {
                val spec = specs[op.pick % specs.size]
                val v = spec.values[op.valuePick % spec.values.size]
                s.copy(
                    options =
                        OptionAssignment(
                            if (v ==
                                spec.values.first()
                            ) {
                                s.options.values - spec.name
                            } else {
                                s.options.values + (spec.name to v)
                            },
                        ),
                )
            }
        }
        is ChangeOp.ToggleProcessor -> {
            val all = ProcessorId.entries.filter { !s.processors.placement.isKmp || it.kmp }
            val id = all[op.pick % all.size]
            val next = if (id in s.processors.members) s.processors.order - id else s.processors.order + id
            if (next.isEmpty()) s else s.copy(processors = s.processors.copy(order = next))
        }
        else -> s.copy(project = normalize(applyToProject(s.project, op)))
    }

private fun applyToProject(
    p: ProjectSpec,
    op: ChangeOp,
): ProjectSpec {
    val classes = p.classIds()
    return when (op) {
        is ChangeOp.AddClass -> {
            val donor = generateProject(Random(op.seed), ProjectShape(maxModules = 1, maxFilesPerModule = 1, maxDeclsPerFile = 1))
            val decl =
                donor.modules
                    .flatMap { it.files }
                    .filterIsInstance<KotlinFileSpec>()
                    .flatMap { it.decls }
                    .firstOrNull()
                    ?: return p
            val newId = "X${op.pick}_${op.seed and 0xFFFF}"
            val renamed = if (decl is KClassDecl) rewriteIds(decl, newId) else return p
            val files = p.modules.flatMap { m -> m.files.mapIndexedNotNull { i, f -> if (f is KotlinFileSpec) m.name to i else null } }
            if (files.isEmpty()) return p
            val (mod, fi) = files[op.pick % files.size]
            p.copy(
                modules =
                    p.modules.map { m ->
                        if (m.name != mod) {
                            m
                        } else {
                            m.copy(
                                files =
                                    m.files.mapIndexed { i, f ->
                                        if (i == fi &&
                                            f is KotlinFileSpec
                                        ) {
                                            f.copy(decls = f.decls + renamed)
                                        } else {
                                            f
                                        }
                                    },
                            )
                        }
                    },
            )
        }
        is ChangeOp.RemoveDecl -> {
            val tops = p.modules.flatMap { m -> m.files.filterIsInstance<KotlinFileSpec>().flatMap { f -> f.decls.map { it.id } } }
            val all = (classes + tops).distinct()
            if (all.isEmpty()) return p
            val target = all[op.pick % all.size]
            p.mapClass(target) { null }.mapTopLevelNotNull { if (it.id == target) null else it }
        }
        is ChangeOp.Rename -> {
            if (classes.isEmpty()) return p
            val id = classes[op.pick % classes.size]
            p.mapClass(id) { it.copy(name = "${it.id}R${op.suffix}") }
        }
        is ChangeOp.ToggleNullable -> onProperty(p, op.pick) { it.copy(type = toggleNullable(it.type)) }
        is ChangeOp.ChangeType ->
            onProperty(p, op.pick) {
                it.copy(
                    type =
                        if (it.type ==
                            TypeRef.INT
                        ) {
                            TypeRef.Builtin(me.tbsten.analysis.code.gen.pbt.source.BuiltinType.STRING)
                        } else {
                            TypeRef.INT
                        },
                )
            }
        is ChangeOp.ChangeBody -> onFunction(p, op.pick) { it.copy(bodyVersion = it.bodyVersion + 1) }
        is ChangeOp.ToggleAnnotation -> {
            val annos = p.classIds().filter { id -> p.findClass(id)?.kind == ClassKind.ANNOTATION }
            val targets = classes.filter { id -> p.findClass(id)?.kind != ClassKind.ANNOTATION }
            if (annos.isEmpty() || targets.isEmpty()) return p
            val a = annos[op.annotationPick % annos.size]
            p.mapClass(targets[op.pick % targets.size]) { d ->
                if (d.annotations.any { it.annotationId == a }) {
                    d.copy(annotations = d.annotations.filter { it.annotationId != a })
                } else {
                    d.copy(
                        annotations =
                            d.annotations + AnnotationUse(a),
                    )
                }
            }
        }
        is ChangeOp.MoveFile -> {
            val files = p.modules.flatMap { m -> m.files.indices.map { m.name to it } }
            if (files.isEmpty()) return p
            val (mod, fi) = files[op.pick % files.size]
            p.copy(
                modules =
                    p.modules.map { m ->
                        if (m.name != mod) {
                            m
                        } else {
                            m.copy(
                                files =
                                    m.files.mapIndexed { i, f ->
                                        if (i != fi) {
                                            f
                                        } else {
                                            val pkg = "gen.${m.name}.p${op.toPackage}"
                                            when (f) {
                                                is KotlinFileSpec -> f.copy(pkg = pkg)
                                                is JavaFileSpec -> f.copy(pkg = pkg)
                                            }
                                        }
                                    },
                            )
                        }
                    },
            )
        }
        is ChangeOp.ToggleDependency -> {
            if (p.modules.size < 2) return p
            val i = 1 + op.pick % (p.modules.size - 1)
            val m = p.modules[i]
            val target = p.modules[op.target % i].name
            val deps = if (target in m.dependsOn) m.dependsOn - target else m.dependsOn + target
            p.copy(modules = p.modules.mapIndexed { j, x -> if (j == i) x.copy(dependsOn = deps) else x })
        }
        is ChangeOp.ChangeOption, is ChangeOp.ToggleProcessor -> p
    }
}

private fun toggleNullable(t: TypeRef): TypeRef =
    when (t) {
        is TypeRef.Builtin -> t.copy(nullable = !t.nullable)
        is TypeRef.Decl -> t.copy(nullable = !t.nullable)
        is TypeRef.Java -> t
        is TypeRef.Param -> t.copy(nullable = !t.nullable)
    }

private fun rewriteIds(
    d: KClassDecl,
    prefix: String,
): KClassDecl {
    var n = 0

    fun walk(x: KClassDecl): KClassDecl {
        val id = if (n == 0) prefix else "${prefix}_$n"
        n++
        return x.copy(
            id = id,
            name = id,
            // 足すクラスの中身は、元のプロジェクトの宣言を参照しない（参照は normalize が Int に直す）
            nested = x.nested.map { walk(it) },
            members =
                x.members.map { m ->
                    when (m) {
                        is PropertyMember -> m.copy(name = "${id}_${m.name}")
                        is FunctionMember -> m.copy(name = "${id}_${m.name}")
                    }
                },
            ctorParams = x.ctorParams.map { c: ParamSpec -> if (c.kind == PropKind.PLAIN) c else c.copy(name = "${id}_${c.name}") },
            companion = x.companion.map { m -> if (m is PropertyMember) m.copy(name = "${id}_${m.name}") else m },
        )
    }
    return walk(d)
}

private fun ProjectSpec.findClass(id: String): KClassDecl? {
    var found: KClassDecl? = null
    mapClass(id) {
        found = it
        it
    }
    return found
}

private fun onProperty(
    p: ProjectSpec,
    pick: Int,
    f: (PropertyMember) -> PropertyMember,
): ProjectSpec {
    val targets =
        p.classIds().flatMap { id ->
            p
                .findClass(id)!!
                .members
                .filterIsInstance<PropertyMember>()
                .map { id to it.name }
        }
    if (targets.isEmpty()) return p
    val (cid, name) = targets[pick % targets.size]
    return p.mapClass(cid) { d -> d.copy(members = d.members.map { if (it is PropertyMember && it.name == name) f(it) else it }) }
}

private fun onFunction(
    p: ProjectSpec,
    pick: Int,
    f: (FunctionMember) -> FunctionMember,
): ProjectSpec {
    val targets =
        p.classIds().flatMap { id ->
            p
                .findClass(id)!!
                .members
                .filterIsInstance<FunctionMember>()
                .map { id to it.name }
        }
    if (targets.isEmpty()) return p
    val (cid, name) = targets[pick % targets.size]
    return p.mapClass(cid) { d -> d.copy(members = d.members.map { if (it is FunctionMember && it.name == name) f(it) else it }) }
}

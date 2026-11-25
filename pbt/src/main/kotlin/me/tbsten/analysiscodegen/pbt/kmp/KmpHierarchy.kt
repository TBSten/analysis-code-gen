package me.tbsten.analysiscodegen.pbt.kmp

import io.kotest.property.Arb
import io.kotest.property.Shrinker
import io.kotest.property.arbitrary.arbitrary

/** KMP のターゲット。[family] は既定の階層テンプレートでまとめられる単位。 */
public enum class KmpTarget(
    public val sourceSet: String,
    public val family: String,
    /** mac のランナーが要るか（毎時の実行に混ぜる割合を決めるのに使う） */
    public val needsMac: Boolean = false,
) {
    JVM("jvmMain", "jvm"),
    ANDROID("androidMain", "jvm"),
    JS("jsMain", "web"),
    WASM_JS("wasmJsMain", "web"),
    IOS_ARM64("iosArm64Main", "apple", needsMac = true),
    IOS_SIMULATOR_ARM64("iosSimulatorArm64Main", "apple", needsMac = true),
    MACOS_ARM64("macosArm64Main", "apple", needsMac = true),
    LINUX_X64("linuxX64Main", "native"),
    MINGW_X64("mingwX64Main", "native"),
}

/** SourceSet の木。根は常に `commonMain`。葉はターゲットごとに 1 つ。 */
public data class KmpHierarchy(
    val targets: List<KmpTarget>,
    /** SourceSet 名 → 親の名前（commonMain は親なし） */
    val parents: Map<String, String?>,
) {
    val sourceSets: Set<String> get() = parents.keys

    /** 自分と祖先（この SourceSet から見える宣言の置き場）。壊れた木（循環）でも止まるよう、SourceSet の数 + 1 で打ち切る */
    public fun visibleFrom(sourceSet: String): List<String> = generateSequence(sourceSet) { parents[it] }.take(parents.size + 1).toList()

    /** この SourceSet のコードがコンパイルされるターゲット */
    public fun targetsOf(sourceSet: String): List<KmpTarget> = targets.filter { sourceSet in visibleFrom(it.sourceSet) }

    public val intermediates: List<String> get() = sourceSets.filter { it != COMMON && KmpTarget.entries.none { t -> t.sourceSet == it } }

    /** 構造の不変条件。空なら妥当。 */
    public fun problems(): List<String> {
        val out = mutableListOf<String>()
        if (targets.isEmpty()) out += "ターゲットが無い"
        if (parents[COMMON] != null || COMMON !in parents) out += "commonMain が根でない"
        for (t in targets) if (t.sourceSet !in parents) out += "${t.sourceSet} が無い"
        for ((s, p) in parents) {
            if (s != COMMON && (p == null || p !in parents)) out += "$s の親 $p が無い"
            if (visibleFrom(s).size > parents.size) out += "$s から根へたどれない（循環）"
        }
        val leaves = targets.map { it.sourceSet }.toSet()
        for (s in sourceSets) {
            if (s != COMMON && s !in leaves && targetsOf(s).isEmpty()) out += "$s はどのターゲットにも入らない"
        }
        return out
    }

    public companion object {
        public const val COMMON: String = "commonMain"
    }
}

/**
 * KMP 階層の Arb（PBT-04 の入力）。ターゲットの空でない部分集合を選び、
 * 葉をランダムにまとめて中間 SourceSet を作る（同じ family をまとめやすくする。既定テンプレートの `appleMain` などに相当）。
 */
public fun Arb.Companion.kmpHierarchy(): Arb<KmpHierarchy> =
    arbitrary(edgecases = listOf(build(listOf(KmpTarget.JVM), emptyList())), shrinker = KmpHierarchyShrinker) { rs ->
        val r = rs.random
        val targets =
            KmpTarget.entries
                .filter {
                    r.nextInt(
                        3,
                    ) == 0
                }.ifEmpty { listOf(KmpTarget.entries[r.nextInt(KmpTarget.entries.size)]) }
        // 中間 SourceSet: family ごとにまとめるか、ランダムな 2 つ以上の組
        val groups = mutableListOf<Pair<String, List<String>>>()
        var roots = targets.map { it.sourceSet }
        var k = 0
        targets.groupBy { it.family }.forEach { (family, ts) ->
            if (ts.size >= 2 && r.nextBoolean()) {
                val name = FAMILY_GROUP.getValue(family)
                groups += name to ts.map { it.sourceSet }
                roots = roots - ts.map { it.sourceSet }.toSet() + name
            }
        }
        while (roots.size >= 3 && r.nextInt(3) == 0) {
            val pick = roots.shuffled(r).take(2 + r.nextInt(roots.size - 2))
            val name = "group${k++}Main"
            groups += name to pick
            roots = roots - pick.toSet() + name
        }
        build(targets, groups)
    }

/** family をまとめる中間 SourceSet の名前（既定の階層テンプレートに合わせる。jvm と android は template に無いので独自名） */
private val FAMILY_GROUP = mapOf("jvm" to "jvmAndAndroidMain", "web" to "webMain", "apple" to "appleMain", "native" to "nativeMain")

private fun build(
    targets: List<KmpTarget>,
    groups: List<Pair<String, List<String>>>,
): KmpHierarchy {
    val parents = LinkedHashMap<String, String?>()
    parents[KmpHierarchy.COMMON] = null
    targets.forEach { parents[it.sourceSet] = KmpHierarchy.COMMON }
    groups.forEach { (g, children) ->
        parents[g] = KmpHierarchy.COMMON
        children.forEach { parents[it] = g }
    }
    return KmpHierarchy(targets, parents)
}

public object KmpHierarchyShrinker : Shrinker<KmpHierarchy> {
    override fun shrink(value: KmpHierarchy): List<KmpHierarchy> {
        val out = mutableListOf<KmpHierarchy>()
        // 中間を外す（子を親へ付け替える）
        for (m in value.intermediates) {
            val parent = value.parents[m]
            out += value.copy(parents = value.parents.filterKeys { it != m }.mapValues { (_, p) -> if (p == m) parent else p })
        }
        // ターゲットを外す（空になった中間も外す）
        if (value.targets.size > 1) {
            for (t in value.targets) {
                val targets = value.targets - t
                var parents: Map<String, String?> = value.parents - t.sourceSet
                val h0 = KmpHierarchy(targets, parents)
                val empty = h0.intermediates.filter { h0.targetsOf(it).isEmpty() }.toSet()
                parents = parents.filterKeys { it !in empty }
                out += KmpHierarchy(targets, parents)
            }
        }
        return out.filter { it.problems().isEmpty() }
    }
}

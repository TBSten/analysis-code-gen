package poc.p02

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * 検証用の入力と、2 段のマルチラウンドを起こす擬似 Processor。
 *
 * ユーザーコード（n ファイル）は、まだ存在しない生成物を参照している。
 * - Round 1: `@Gen` の付いたクラスごとに、新しいパッケージ `gen.r1.pK` に `UserNFactory`（`@Gen2` 付き）を生成する。
 *   3 件に 1 件は、既存パッケージ `app.pK` に上位型 `UserNBase` も生成する（既存クラスの上位型の解決結果が変わる）。
 * - Round 2: Round 1 の生成物のうち `@Gen2` の付いたものごとに、`gen.r2.pK` に拡張関数 `twice()` を生成する。
 * - Round 3: 新しい対象が無いので終わる。
 * 最終状態ではエラー 0 件になる。
 */
object Scenario {
    const val PACKAGES = 10

    fun writeSources(srcRoot: Path, n: Int) {
        srcRoot.resolve("app/ann").createDirectories()
        srcRoot.resolve("app/ann/Annotations.kt").writeText(
            """
            package app.ann

            annotation class Gen
            annotation class Gen2
            """.trimIndent() + "\n",
        )
        for (i in 0 until n) {
            val k = i % PACKAGES
            val dir = srcRoot.resolve("app/p$k").createDirectories()
            val hasBase = i % 3 == 0
            dir.resolve("User$i.kt").writeText(
                """
                package app.p$k

                import app.ann.Gen
                import gen.r1.p$k.User${i}Factory
                import gen.r2.p$k.twice

                @Gen
                class User$i(val id: Int)${if (hasBase) " : User${i}Base" else ""} {
                    fun label(): String = "user-" + id
                }

                fun use$i(): Int {
                    val u = User${i}Factory.create()
                    val t = User${i}Factory.twice()
                    return u.id + t.size${if (hasBase) " + u.baseValue()" else ""}
                }
                """.trimIndent() + "\n",
            )
        }
    }

    /** ラウンドの処理: 対象ファイルからアノテーション付きクラスを探して生成物を書き、書いたファイルを返す。 */
    fun process(session: KaSession, round: Int, targets: List<KtFile>, genRoot: Path): List<Path> = with(session) {
        val wanted = if (round == 1) "app.ann.Gen" else "app.ann.Gen2"
        val out = mutableListOf<Path>()
        for (file in targets) {
            for (cls in file.symbol.fileScope.classifiers.filterIsInstance<KaNamedClassSymbol>()) {
                if (cls.annotations.none { it.classId?.asFqNameString() == wanted }) continue
                val name = cls.name.asString()
                val pkg = cls.classId!!.packageFqName.asString()
                when (round) {
                    1 -> {
                        val i = name.removePrefix("User").toInt()
                        val k = pkg.removePrefix("app.p")
                        out.add(write(
                            genRoot, "gen/r1/p$k/${name}Factory.kt",
                            """
                            package gen.r1.p$k

                            import app.ann.Gen2
                            import $pkg.$name

                            @Gen2
                            object ${name}Factory {
                                fun create(): $name = $name($i)
                            }
                            """.trimIndent() + "\n",
                        ))
                        if (i % 3 == 0) {
                            out.add(write(
                                genRoot, "app/p$k/${name}Base.kt",
                                """
                                package $pkg

                                interface ${name}Base {
                                    fun baseValue(): Int = $i
                                }
                                """.trimIndent() + "\n",
                            ))
                        }
                    }
                    2 -> {
                        val k = pkg.removePrefix("gen.r1.p")
                        val user = name.removeSuffix("Factory")
                        out.add(write(
                            genRoot, "gen/r2/p$k/${name}Ext.kt",
                            """
                            package gen.r2.p$k

                            import app.p$k.$user
                            import $pkg.$name

                            fun $name.twice(): List<$user> = listOf(create(), create())
                            """.trimIndent() + "\n",
                        ))
                    }
                }
            }
        }
        out
    }

    private fun write(root: Path, rel: String, text: String): Path {
        val p = root.resolve(rel)
        p.parent.createDirectories()
        p.writeText(text)
        return p
    }
}

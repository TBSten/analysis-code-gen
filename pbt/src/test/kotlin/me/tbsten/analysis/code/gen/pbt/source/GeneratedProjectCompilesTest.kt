package me.tbsten.analysis.code.gen.pbt.source

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import me.tbsten.analysis.code.gen.pbt.compile.JvmCompileCheck
import me.tbsten.analysis.code.gen.pbt.run.acgCheckAll
import me.tbsten.analysis.code.gen.testfoundation.UnitSpec

class GeneratedProjectCompilesTest : UnitSpec({
    case("PBT-01", "ランダムな Kotlin ソースの生成器（宣言・ジェネリクス・アノテーション・ネスト・Java 混在）が、毎回コンパイルできるプロジェクトを作る") {
        acgCheckAll(
            "PBT-01 生成したプロジェクトはコンパイルできる",
            Arb.projectSpec(),
            generatorVersion = "source-$SOURCE_GENERATOR_VERSION",
            prIterations = 30,
            labels = { it.features() },
        ) { spec ->
            val result = JvmCompileCheck.compile(render(spec))
            withClue(result.log) { result.ok shouldBe true }
        }
    }

    "生成器は宣言の種類・ジェネリクス・注釈・入れ子・Java 混在・複数モジュールをすべて出す（偏りの検査）" {
        val rs = RandomSource.seeded(40L)
        val seen = HashSet<String>()
        repeat(400) { seen += rs.projectSpec().features() }
        seen shouldContainAll
            listOf(
                "decl=class",
                "decl=data",
                "decl=interface",
                "decl=object",
                "decl=enum",
                "decl=sealed",
                "decl=value",
                "decl=annotation",
                "decl=typealias",
                "decl=function",
                "decl=property",
                "decl=const",
                "vis=public",
                "vis=internal",
                "vis=private",
                "generics=class",
                "generics=function",
                "generics=variance-out",
                "generics=variance-in",
                "generics=bound",
                "annotation=class",
                "annotation=site-field",
                "annotation=site-get",
                "annotation=site-param",
                "annotation=site-property",
                "nesting=nested",
                "nesting=inner",
                "misc=inheritance",
                "misc=sealed-subclass",
                "misc=companion",
                "ctor=default-arg",
                "types=nullable",
                "fn=extension",
                "java=mixed",
                "layout=multi-module",
                "layout=multi-file",
            )
    }

    "生成した値・縮小の候補はすべて normalize の不動点（どの段階でもコンパイルできる形）" {
        val rs = RandomSource.seeded(41L)
        repeat(60) {
            val spec = rs.projectSpec()
            spec.isValid() shouldBe true
            ProjectShrinker.shrink(spec).forEach { c -> withClue(c) { c.isValid() shouldBe true } }
        }
    }
})

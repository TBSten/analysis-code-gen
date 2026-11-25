package me.tbsten.analysiscodegen.testprocessors

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File

/** フィクスチャに置いた TpAnnotations.kt のコピーが、正（TpAnnotationsSource）と一致すること。 */
class TpAnnotationsSourceTest : FreeSpec({
    val root = File(System.getProperty("acg.repoRoot") ?: error("acg.repoRoot が無い"))
    val copies =
        File(root, "fixtures/projects")
            .walkTopDown()
            .onEnter { it.name != "build" && it.name != ".gradle" }
            .filter { it.name == TpAnnotationsSource.FILE_NAME }
            .toList()

    "フィクスチャに 1 つ以上ある" {
        copies.shouldNotBeEmpty()
    }

    copies.forEach { file ->
        "${file.relativeTo(root)} が正と一致する" {
            file.readText() shouldBe TpAnnotationsSource.content
        }
    }
})

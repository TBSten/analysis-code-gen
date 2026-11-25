package me.tbsten.analysiscodegen.e2e

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

// 足場の確認用。テスト基底クラス（ACG-37）ができたら置き換える
class ScaffoldSmokeTest : FreeSpec({
    "kotest が動く" {
        (1 + 1) shouldBe 2
    }
})

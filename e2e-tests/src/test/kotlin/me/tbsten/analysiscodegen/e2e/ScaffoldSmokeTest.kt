package me.tbsten.analysiscodegen.e2e

import io.kotest.matchers.shouldBe
import me.tbsten.analysiscodegen.testfoundation.E2ESpec

// 足場の確認用（ケース ID なしの補助テスト）。E2E のケースが入ったら消してよい
class ScaffoldSmokeTest : E2ESpec({
    "E2E の基底クラスで kotest が動く" {
        (1 + 1) shouldBe 2
    }
})

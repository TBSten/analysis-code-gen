package me.tbsten.analysiscodegen.differential

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import me.tbsten.analysiscodegen.testfoundation.E2ESpec
import me.tbsten.analysiscodegen.testprocessors.BadRender

class DifferentialRunnerTest : E2ESpec({
    case("XD-03", "3 つの実行系（K / B / N）を同じ入力で動かして出力を集めるランナーが、固定の入力 1 件で動く") {
        val harness = DifferentialHarness.default(cache = null)

        val result = harness.run(TpInputs.fixed())

        // 3 つとも呼ばれ、結果が集まる（B / N は実装されるまで「動かせない」とその理由）
        result.runs.keys shouldBe setOf(EngineKind.K, EngineKind.B, EngineKind.N)
        listOf(EngineKind.B, EngineKind.N).forEach { e ->
            val run = result.runs.getValue(e)
            if (run is EngineRun.Unavailable) run.reason.isNotBlank() shouldBe true
        }
        result.comparisons.map { it.right } shouldBe listOf(EngineKind.B, EngineKind.N)

        // K は実際に動き、生成物・ラウンドが集まる
        val k = result.runs.getValue(EngineKind.K).shouldBeInstanceOf<EngineRun.Completed>()
        k.output.success shouldBe true
        val out = result.normalized.getValue(EngineKind.K)
        out.files.keys shouldContainAll
            listOf(
                "kotlin/com/example/UserMapper.kt",
                "kotlin/me/tbsten/analysiscodegen/tp/generated/TpRegistry.kt",
                "kotlin/com/example/PipelineChain1.kt",
                "kotlin/com/example/PipelineChain2.kt",
            )
        out.sources.keys.any { it.startsWith("java/") } shouldBe true
        out.resources.keys.any { it.startsWith("resources/META-INF/services/") } shouldBe true
        // 正規化の後は実行ごとの絶対パスが残らない
        out.files.values.forEach { it shouldNotContain k.output.roots.first() }
        // TP-Chain は 1 ラウンドに 1 段なので、Chain1 と Chain2 は別のラウンドに出る
        val rounds = out.rounds.shouldNotBeNull()
        rounds shouldHaveAtLeastSize 3
        val roundOf = rounds.flatMap { r -> r.generatedFiles.map { it to r.index } }.toMap()
        roundOf shouldContainKey "kotlin/com/example/PipelineChain1.kt"
        (roundOf.getValue("kotlin/com/example/PipelineChain2.kt") > roundOf.getValue("kotlin/com/example/PipelineChain1.kt")) shouldBe true
        // 生成ファイルはすべてどこかのラウンドに出ている
        roundOf.keys shouldBe out.files.keys
    }

    "Logger の出力は位置付きで Processor のものとして集まり、ビルドの失敗も結果として返る" {
        val input =
            DiffInput(
                name = "logger",
                sources =
                    listOf(
                        TpInputs.annotations,
                        DiffSource(
                            "com/example/Target.kt",
                            "package com.example\n\nimport me.tbsten.analysiscodegen.tp.BadTarget\n\n@BadTarget\nclass Target\n",
                        ),
                    ),
                processors = listOf(TpInputs.bad),
                options = mapOf("tp.processors" to "Bad", "tp.bad.modes" to "log-error"),
                expectation = Expectation.Invalid,
            )

        val run = DifferentialHarness.default(cache = null).run(input)

        val out = run.normalized.getValue(EngineKind.K)
        out.success shouldBe false
        out.logs shouldHaveSize 1
        out.logs.single().message shouldContain BadRender.LOG_ERROR_MESSAGE
        out.logs.single().severity shouldBe DiagnosticRecord.Severity.Error
        // KSP2 はクラス宣言の位置としてアノテーションの次の行（class の行）を返す
        out.logs.single().location shouldBe "com/example/Target.kt:6"
        // Logger の出力はコンパイラの診断に重ねて数えない
        out.diagnostics.filter { !it.fromProcessor && BadRender.LOG_ERROR_MESSAGE in it.message } shouldHaveSize 0
    }
})

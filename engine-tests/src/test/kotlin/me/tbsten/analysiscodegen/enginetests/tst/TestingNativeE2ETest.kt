package me.tbsten.analysiscodegen.enginetests.tst

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysiscodegen.enginetests.support.processorProvider
import me.tbsten.analysiscodegen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysiscodegen.processor.Dependencies
import me.tbsten.analysiscodegen.testfoundation.E2ESpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import me.tbsten.analysiscodegen.testing.TestEngine
import me.tbsten.analysiscodegen.testing.runCodegenTest
import org.jetbrains.kotlin.psi.KtClass

// 5.21 testing（TST）のうち Native Processor を渡すもの。Processor を書くのに AA の型が要るので :engine-tests に置く（ADR-034・037）。
// TST-02〜08 は e2e-tests の e2e.tst

/** ラウンド 1 で、各クラスに `<Class>Greeting` を作り、そのクラスを指して warn を出す。 */
private fun greet(context: AnalysisCodegenRoundContext) {
    if (context.round != 1) return
    for (file in context.allFiles) {
        val pkg = file.packageFqName.asString()
        for (klass in file.declarations.filterIsInstance<KtClass>()) {
            val name = "${klass.name}Greeting"
            context.codeGenerator.generateKotlin(
                packageName = pkg,
                fileName = name,
                dependencies = Dependencies.isolating(file),
                content = "package $pkg\n\nobject $name {\n    const val TEXT: String = \"Hello, ${klass.name}\"\n}\n",
            )
            context.logger.warn("generated $name", klass)
        }
    }
}

class TestingNativeE2ETest : E2ESpec({
    case(
        "TST-01",
        "ソース文字列を渡して Native Processor を実行し、生成物と診断を取得できる",
        pending = PendingCase(Milestone.M1),
    ) {
        val result =
            runCodegenTest(TestEngine.AnalysisCodegen) {
                kotlin(
                    "com/example/User.kt",
                    """
                    package com.example

                    class User(val name: String)
                    """,
                )
                native(processorProvider(process = ::greet))
            }

        result.isSuccess shouldBe true
        // 生成物: パスの末尾で引ける
        result.generatedFile("com/example/UserGreeting.kt") shouldContain "object UserGreeting"
        // 診断: Processor の warn がそのまま届く
        result.warnings.single().message shouldContain "generated UserGreeting"
        result.errors.shouldBeEmpty()
        // 生成物はユーザーのコードと一緒にコンパイルされ、読み込める
        result.loadClass("com.example.UserGreeting").getField("TEXT").get(null) shouldBe "Hello, User"
    }

    case(
        "TST-01",
        "ソース文字列を渡して Native Processor を実行し、生成物と診断を取得できる（error ならビルドは失敗）",
        pending = PendingCase(Milestone.M1),
    ) {
        val result =
            runCodegenTest(TestEngine.AnalysisCodegen) {
                kotlin("com/example/Broken.kt", "package com.example\n\nclass Broken")
                native(
                    processorProvider { context ->
                        if (context.round == 1) {
                            val klass =
                                context.allFiles
                                    .flatMap { it.declarations }
                                    .filterIsInstance<KtClass>()
                                    .single()
                            context.logger.error("Broken は使えない", klass)
                        }
                    },
                )
            }

        result.isSuccess shouldBe false
        result.errors.single().message shouldContain "Broken は使えない"
        result.generatedFiles.keys.shouldBeEmpty()
    }
})

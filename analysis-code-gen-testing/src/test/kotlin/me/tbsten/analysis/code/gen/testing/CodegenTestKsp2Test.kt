package me.tbsten.analysis.code.gen.testing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val HELLO_ANNOTATION = "package com.example\n\nannotation class Hello"

// testing の KSP2 経路（差分の K 側、ADR-021）が実際に動くことの確認。
// TST-01〜03 の本体は Native / Bridge が要るので、ACG-44〜46 で TestEngine.AnalysisCodegen の保留テストとして書く
class CodegenTestKsp2Test : FreeSpec({
    // TST-01・02 の K 側
    "ソース文字列と KSP Processor から生成物と診断が取れる" {
        val result =
            runCodegenTest(TestEngine.Ksp2) {
                kotlin("com/example/Hello.kt", HELLO_ANNOTATION)
                kotlin("com/example/Foo.kt", "package com.example\n\n@Hello class Foo")
                ksp(HelloProcessorProvider())
                options["hello.prefix"] = "Hi"
            }

        result.isSuccess shouldBe true
        result.generatedFiles.keys shouldContainExactly setOf("kotlin/com/example/FooHello.kt")
        result.generatedFile("FooHello.kt") shouldContain "\"Hi, Foo\""
        result.errors shouldHaveSize 0
    }

    // TST-03 の K 側
    "生成物をコンパイルし、生成クラスを読み込んで実行できる" {
        val result =
            runCodegenTest(TestEngine.Ksp2) {
                kotlin("com/example/Hello.kt", HELLO_ANNOTATION)
                kotlin("com/example/Foo.kt", "package com.example\n\n@Hello class Foo")
                ksp(HelloProcessorProvider())
            }

        val cls = result.loadClass("com.example.FooHello")
        val instance = cls.getField("INSTANCE").get(null)
        cls.getMethod("hello").invoke(instance) shouldBe "Hello, Foo"
    }

    "Processor のエラーは失敗と診断になる" {
        val result =
            runCodegenTest(TestEngine.Ksp2) {
                kotlin("com/example/Hello.kt", HELLO_ANNOTATION)
                kotlin("com/example/Foo.kt", "package com.example\n\n@Hello class Foo")
                ksp(HelloProcessorProvider())
                options["hello.fail"] = "true"
            }

        result.isSuccess shouldBe false
        result.errors.joinToString { it.message } shouldContain "hello.fail が指定された: Foo"
        shouldThrow<IllegalStateException> { result.classLoader }
    }

    "利用者コードのコンパイルエラーも失敗になる" {
        val result =
            runCodegenTest(TestEngine.Ksp2) {
                kotlin("com/example/Broken.kt", "package com.example\n\nval x: Int = \"s\"")
            }

        result.isSuccess shouldBe false
        result.errors.isNotEmpty() shouldBe true
    }

    // TST-05 の K 側
    "シナリオは各ステップをクリーンに実行し、変更が生成物に反映される" {
        val steps =
            runCodegenScenario(TestEngine.Ksp2) {
                initial {
                    kotlin("com/example/Hello.kt", HELLO_ANNOTATION)
                    kotlin("com/example/A.kt", "package com.example\n\n@Hello class A")
                    ksp(HelloProcessorProvider())
                }
                step("B を足す") { kotlin("com/example/B.kt", "package com.example\n\n@Hello class B") }
                step("A を消す") { delete("com/example/A.kt") }
                step("挨拶を変える") { option("hello.prefix", "Yo") }
            }

        steps.map { it.stepName } shouldContainExactly listOf("initial", "B を足す", "A を消す", "挨拶を変える")
        steps.map {
            it.result.generatedFiles.keys
                .map { k -> k.substringAfterLast('/') }
        } shouldContainExactly
            listOf(
                listOf("AHello.kt"),
                listOf("AHello.kt", "BHello.kt"),
                listOf("BHello.kt"),
                listOf("BHello.kt"),
            )
        steps.last().result.generatedFile("BHello.kt") shouldContain "\"Yo, B\""
    }

    "生成物の並びは辞書順（決定性）" {
        val result =
            runCodegenTest(TestEngine.Ksp2) {
                kotlin("com/example/Hello.kt", HELLO_ANNOTATION)
                listOf("Zed", "Alpha", "Mid").forEach { kotlin("com/example/$it.kt", "package com.example\n\n@Hello class $it") }
                ksp(HelloProcessorProvider())
            }

        result.generatedFiles.keys.toList() shouldBe result.generatedFiles.keys.sorted()
    }

    "Ksp2 は SourceSet 1 つだけ。KMP の指定は分かるエラーにする" {
        shouldThrow<IllegalArgumentException> {
            runCodegenTest(TestEngine.Ksp2) {
                sourceSet("commonMain") { kotlin("A.kt", "class A") }
                sourceSet("jvmMain") {
                    dependsOn += "commonMain"
                    kotlin("B.kt", "class B")
                }
            }
        }.message shouldContain "SourceSet 1 つ"
    }

    "AnalysisCodegen エンジンはエンジン実装（P2）までは未実装を明示する" {
        shouldThrow<NotImplementedError> {
            runCodegenTest(TestEngine.AnalysisCodegen) { kotlin("A.kt", "class A") }
        }
    }
})

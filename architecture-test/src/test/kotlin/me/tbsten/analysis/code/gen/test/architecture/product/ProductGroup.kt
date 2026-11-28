@file:OptIn(ExperimentalKatachiApi::class)

package me.tbsten.analysis.code.gen.test.architecture.product

import me.tbsten.analysis.code.gen.test.architecture.INTELLIJ
import me.tbsten.analysis.code.gen.test.architecture.KOTLIN_COMPILER
import me.tbsten.analysis.code.gen.test.architecture.mainKotlin
import me.tbsten.analysis.code.gen.test.architecture.portedFromHeader
import me.tbsten.katachi.ExperimentalKatachiApi
import me.tbsten.katachi.dsl.DeclarationContainerScope
import me.tbsten.katachi.dsl.kotlin.ktFile
import me.tbsten.katachi.konsist.declarations.ktFileContent

// 利用者に届くモジュールのコード（ADR-012、ADR-042）。モジュールと公開範囲の対応は README のモジュール表。
// 各 Role の ktFileContent は「パッケージ = ディレクトリ」と、ADR-012 の依存方向を import の許可リストで守る
fun DeclarationContainerScope.productGroup() = "Product".group {
    summary = "利用者に届くモジュールのコード。公開しうるモジュールは analysis-code-gen- で始まる"

    publicApiGroup()
    engineGroup()

    "TestingKct".role {
        summary = "kotlin-compile-testing の fork から移植したコード（ADR-021）。手で直さず porting/kct/vendor.py で取り込む"
        documented = false
        layout { mainKotlin("analysis-code-gen-testing-kct", "testing/kct") / "*".ktFile() }
        portedFromHeader()
    }
    "TestingKctServiceRegistration".role {
        summary = "移植した kct のコンパイラプラグインを ServiceLoader に登録するファイル"
        documented = false
        layout {
            "analysis-code-gen-testing-kct/src/main/resources/META-INF/services" / "org.jetbrains.kotlin.compiler.plugin.*".file()
        }
    }
}

private fun DeclarationContainerScope.publicApiGroup() = "PublicApi".group {
    summary = "利用者が直接使う公開モジュール（BCV の対象）"

    "ProcessorApi".role {
        summary = "Native Processor 向けの公開 API（ADR-013）。AA の型を compileOnlyApi で見せる"
        layout { mainKotlin("analysis-code-gen-processor-api", "processor") / "*".ktFile() }
        "AA と自分のパッケージだけを使う".ktFileContent {
            `package`(matchingDirectory)
            fileAnnotation("OptIn")
            `import`("java.io.**")
            `import`(KOTLIN_COMPILER)
            `import`(INTELLIJ)
            `import`("me.tbsten.analysis.code.gen.processor.**")
            allow(anythingElse)
        }
    }
    "GradlePlugin".role {
        summary = "KGP / AGP と統合する Gradle plugin と DSL（ADR-016）"
        layout { mainKotlin("analysis-code-gen-gradle-plugin", "gradle") / "AnalysisCodegen*".ktFile() }
        "ビルドスクリプトの classpath に AA・エンジンを載せない（GRD-21）".ktFileContent {
            `package`(matchingDirectory)
            fileAnnotation("OptIn")
            `import`("org.gradle.**")
            `import`("javax.inject.**")
            // JDK と、project-model の JSON（ProjectModel を Worker へ渡す。ADR-063）。AA・エンジンは引き続き不可
            `import`("java.io.**")
            `import`("java.util.**")
            `import`("java.lang.reflect.**")
            `import`("kotlinx.serialization.**")
            `import`("org.jetbrains.kotlin.gradle.**")
            `import`("me.tbsten.analysis.code.gen.gradle.**")
            `import`("me.tbsten.analysis.code.gen.model.**")
            allow(anythingElse)
        }
    }
    "KspBridgeApi".role {
        summary = "Hybrid Processor の公開 API（実験的。ADR-017）"
        layout { mainKotlin("analysis-code-gen-ksp-bridge-api", "ksp/hybrid") / "*".ktFile() }
        "公式 KSP API・AA・processor-api だけを使う".ktFileContent {
            `package`(matchingDirectory)
            fileAnnotation("OptIn")
            `import`("com.google.devtools.ksp.**")
            `import`(KOTLIN_COMPILER)
            `import`("me.tbsten.analysis.code.gen.processor.**")
            `import`("me.tbsten.analysis.code.gen.ksp.hybrid.**")
            allow(anythingElse)
        }
    }
    "TestingApi".role {
        summary = "利用者向けのテスト補助の公開部分（CodegenTest など。ADR-021）"
        layout {
            val testingDir = mainKotlin("analysis-code-gen-testing", "testing")
            testingDir {
                "{Codegen*,*Engine}".ktFile()
                "gradle" / "Gradle*".ktFile()
            }
        }
        "エンジン・TestKit・kct を包み、利用者に公開する".ktFileContent {
            `package`(matchingDirectory)
            fileAnnotation("OptIn")
            `import`("java.io.**")
            `import`("java.nio.**")
            `import`("org.gradle.testkit.**")
            `import`("com.google.devtools.ksp.**")
            `import`(KOTLIN_COMPILER)
            `import`("me.tbsten.analysis.code.gen.**")
            allow(anythingElse)
        }
    }
    "TestingRunner".role {
        summary = "テスト補助の内部実装。エンジンごとのランナー（*TestRunner）"
        layout { mainKotlin("analysis-code-gen-testing", "testing") / "internal" / "*TestRunner".ktFile() }
        "エンジン・kct を呼ぶ".ktFileContent {
            `package`(matchingDirectory)
            fileAnnotation("OptIn")
            `import`("java.io.**")
            `import`("java.nio.**")
            `import`("com.google.devtools.ksp.**")
            `import`(KOTLIN_COMPILER)
            `import`("me.tbsten.analysis.code.gen.**")
            allow(internal, anythingElse)
            allow(private, anythingElse)
        }
    }
}

private fun DeclarationContainerScope.engineGroup() = "Engine".group {
    summary = "公開 API の裏で動く内部モジュール（@InternalAnalysisCodegenApi）"

    "ProjectModel".role {
        summary = "Gradle・AA に依存しないプロジェクトモデル。JSON で直列化でき、CLI の入力にもなる"
        layout { mainKotlin("analysis-code-gen-project-model", "model") / "*".ktFile() }
        "Gradle にも AA にも依存しない".ktFileContent {
            `package`(matchingDirectory)
            fileAnnotation("OptIn")
            `import`("kotlinx.serialization.**")
            `import`("java.io.**")
            `import`("me.tbsten.analysis.code.gen.model.**")
            allow(anythingElse)
        }
    }
    "EngineSpi".role {
        summary = "Bridge などが処理エンジンへ参加するための SPI（ADR-017）"
        layout { mainKotlin("analysis-code-gen-engine-spi", "engine/spi") / "*".ktFile() }
        "processor-api と project-model の上に載る".ktFileContent {
            `package`(matchingDirectory)
            fileAnnotation("OptIn")
            `import`("java.io.**")
            `import`(KOTLIN_COMPILER)
            `import`(INTELLIJ)
            `import`("me.tbsten.analysis.code.gen.processor.**")
            `import`("me.tbsten.analysis.code.gen.model.**")
            `import`("me.tbsten.analysis.code.gen.engine.spi.**")
            allow(anythingElse)
        }
    }
    "AnalysisEngine".role {
        summary = "AA Standalone の環境構築・Module Graph・無効化（ADR-011）"
        layout { mainKotlin("analysis-code-gen-analysis-engine", "analysis") / "*".ktFile() }
        "ラウンド制御（processing-engine）を知らない".ktFileContent {
            `package`(matchingDirectory)
            fileAnnotation("OptIn")
            `import`("java.io.**")
            `import`(KOTLIN_COMPILER)
            `import`(INTELLIJ)
            `import`("me.tbsten.analysis.code.gen.processor.**")
            `import`("me.tbsten.analysis.code.gen.model.**")
            `import`("me.tbsten.analysis.code.gen.analysis.**")
            allow(anythingElse)
        }
    }
    "IncrementalEngine".role {
        summary = "依存グラフ・入力スナップショット・dirty 計算（ADR-015）"
        layout { mainKotlin("analysis-code-gen-incremental-engine", "incremental") / "*".ktFile() }
        "AA に依存しない（Reference Engine や単体テストで AA なしに回すため）".ktFileContent {
            `package`(matchingDirectory)
            fileAnnotation("OptIn")
            `import`("java.io.**")
            `import`("me.tbsten.analysis.code.gen.processor.**")
            `import`("me.tbsten.analysis.code.gen.model.**")
            `import`("me.tbsten.analysis.code.gen.incremental.**")
            allow(anythingElse)
        }
    }
    "ProcessingEngine".role {
        summary = "ラウンド制御・Processor の読み込み・出力トランザクション（ADR-014）。エンジンの組み立てはここ 1 か所"
        // サブパッケージ engine.spi は engine-spi モジュールなので、ここはパッケージの直下だけ
        layout { mainKotlin("analysis-code-gen-processing-engine", "engine") / "*".ktFile() }
        "エンジンの各部を組み立てる".ktFileContent {
            `package`(matchingDirectory)
            fileAnnotation("OptIn")
            `import`("java.io.**")
            // Gradle の Worker からの入口 EngineEntryPoint（JDK の型と ProjectModel の JSON だけで受ける。ADR-063）
            `import`("java.util.function.**")
            `import`("kotlinx.serialization.**")
            `import`(KOTLIN_COMPILER)
            `import`(INTELLIJ)
            `import`("me.tbsten.analysis.code.gen.**")
            not.`import`("me.tbsten.analysis.code.gen.ksp.**")
            allow(anythingElse)
        }
    }
    "KspBridge".role {
        summary = "KSP API の AA 実装（KSP2 から移植）と Processor Adapter（ADR-017、ADR-018）"
        layout { mainKotlin("analysis-code-gen-ksp-bridge", "ksp/bridge") / "**" / "*".ktFile() }
        "エンジンとは engine-spi だけでやり取りする（ADR-012）".ktFileContent {
            `package`(matchingDirectory)
            fileAnnotation("OptIn")
            `import`("java.io.**")
            `import`("com.google.devtools.ksp.**")
            `import`(KOTLIN_COMPILER)
            `import`(INTELLIJ)
            `import`("me.tbsten.analysis.code.gen.processor.**")
            `import`("me.tbsten.analysis.code.gen.engine.spi.**")
            `import`("me.tbsten.analysis.code.gen.ksp.**")
            allow(anythingElse)
        }
    }
    "Cli".role {
        summary = "Gradle に依存しない実行。project-model の JSON を受けて生成する（ADR-040）"
        layout { mainKotlin("analysis-code-gen-cli", "cli") / "{Main,Cli*}".ktFile() }
        "Gradle に依存しない".ktFileContent {
            `package`(matchingDirectory)
            fileAnnotation("OptIn")
            `import`("java.io.**")
            `import`("kotlinx.serialization.**")
            `import`("me.tbsten.analysis.code.gen.**")
            not.`import`("me.tbsten.analysis.code.gen.gradle.**")
            allow(anythingElse)
        }
    }
}

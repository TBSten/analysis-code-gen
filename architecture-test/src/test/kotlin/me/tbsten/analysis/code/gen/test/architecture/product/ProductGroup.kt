package me.tbsten.analysis.code.gen.test.architecture.product

import me.tbsten.analysis.code.gen.test.architecture.mainKotlin
import me.tbsten.katachi.dsl.DeclarationContainerScope
import me.tbsten.katachi.dsl.kotlin.ktFile

// 利用者に届くモジュールのコード（ADR-012、ADR-042）。モジュールと公開範囲の対応は README のモジュール表
fun DeclarationContainerScope.productGroup() = "Product".group {
    summary = "利用者に届くモジュールのコード。公開しうるモジュールは analysis-code-gen- で始まる"

    "PublicApi".group {
        summary = "利用者が直接使う公開モジュール（BCV の対象）"
        moduleSource("ProcessorApi", "Native Processor 向けの公開 API", "analysis-code-gen-processor-api", "processor")
        moduleSource("GradlePlugin", "KGP / AGP と統合する Gradle plugin と DSL", "analysis-code-gen-gradle-plugin", "gradle")
        moduleSource("KspBridgeApi", "Hybrid Processor の公開 API（実験的）", "analysis-code-gen-ksp-bridge-api", "ksp/hybrid")
        moduleSource(
            "KspBridgeGradlePlugin",
            "kspBridge { } DSL と Bridge ランタイムを足す Gradle plugin",
            "analysis-code-gen-ksp-bridge-gradle-plugin",
            "ksp/gradle",
        )
        "TestingApi".role {
            summary = "利用者向けのテスト補助の公開部分（CodegenTest など）"
            layout {
                val testingDir = mainKotlin("analysis-code-gen-testing", "testing")
                testingDir {
                    "*".ktFile()
                    "gradle" / "*".ktFile()
                }
            }
        }
        "TestingInternal".role {
            summary = "テスト補助の内部実装（エンジンごとのランナー）。利用者は直接使わない"
            layout { mainKotlin("analysis-code-gen-testing", "testing") / "internal" / "*".ktFile() }
        }
    }

    "Engine".group {
        summary = "公開 API の裏で動く内部モジュール（@InternalAnalysisCodegenApi）"
        moduleSource("ProjectModel", "Gradle・AA に依存しないプロジェクトモデル（CLI の入力にもなる）", "analysis-code-gen-project-model", "model")
        moduleSource("EngineSpi", "Bridge などが処理エンジンへ参加するための SPI", "analysis-code-gen-engine-spi", "engine/spi")
        moduleSource("AnalysisEngine", "AA Standalone の環境構築・Module Graph・無効化", "analysis-code-gen-analysis-engine", "analysis")
        moduleSource("IncrementalEngine", "依存グラフと dirty 計算（AA に依存しない）", "analysis-code-gen-incremental-engine", "incremental")
        moduleSource(
            "ProcessingEngine",
            "ラウンド制御・Processor の読み込み・出力トランザクション",
            "analysis-code-gen-processing-engine",
            "engine",
            direct = true,
        )
        moduleSource("KspBridge", "KSP API の AA 実装（KSP2 から移植）と Processor Adapter", "analysis-code-gen-ksp-bridge", "ksp/bridge")
        moduleSource("Cli", "Gradle に依存しない実行（project-model の JSON を受けて生成する）", "analysis-code-gen-cli", "cli")
    }

    "TestingKct".role {
        summary = "kotlin-compile-testing の fork から移植したコード（ADR-021）。手で直さず porting/kct/vendor.py で取り込む"
        documented = false
        layout {
            mainKotlin("analysis-code-gen-testing-kct", "testing/kct") / "*".ktFile()
            "analysis-code-gen-testing-kct/src/main/resources/META-INF/services" / "org.jetbrains.kotlin.compiler.plugin.*".file()
        }
    }
}

/**
 * 1 モジュールの main のソースを 1 Role にする。
 * [direct] はパッケージの根の直下だけを許す（サブパッケージが別モジュールのとき。processing-engine の engine ⊃ engine.spi）
 */
private fun DeclarationContainerScope.moduleSource(
    name: String,
    summary: String,
    module: String,
    subPackage: String,
    direct: Boolean = false,
) = name.role {
    this.summary = summary
    layout {
        if (direct) {
            mainKotlin(module, subPackage) / "*".ktFile()
        } else {
            mainKotlin(module, subPackage) / "**" / "*".ktFile()
        }
    }
}

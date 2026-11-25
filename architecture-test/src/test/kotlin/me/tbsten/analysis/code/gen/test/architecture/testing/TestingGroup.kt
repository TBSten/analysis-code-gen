package me.tbsten.analysis.code.gen.test.architecture.testing

import me.tbsten.analysis.code.gen.test.architecture.CASE_ASPECTS
import me.tbsten.analysis.code.gen.test.architecture.mainKotlin
import me.tbsten.analysis.code.gen.test.architecture.testKotlin
import me.tbsten.katachi.dsl.DeclarationContainerScope
import me.tbsten.katachi.dsl.kotlin.ktFile

// テスト基盤とテストコード（ADR-021〜023、ADR-027、ADR-030、ADR-034、ADR-038）。どれも公開しない内部モジュール
fun DeclarationContainerScope.testingGroup() = "Testing".group {
    summary = "進捗を測るためのテスト基盤とテストコード。公開しない"

    foundationGroup()
    "BenchmarksModule".role {
        summary = "性能測定のモジュール（中身は今後）"
        layout { mainKotlin("benchmarks", "benchmarks") / "*".ktFile() }
    }
    "ArchitectureTest".role {
        summary = "katachi によるファイルの置き場所の定義と検査（ADR-041）"
        layout { testKotlin("architecture-test", "test/architecture") / "**" / "*".ktFile() }
    }
    caseTestsGroup()
    kspCompatibilityGroup()
}

private fun DeclarationContainerScope.foundationGroup() = "Foundation".group {
    summary = "テストを書くための基盤（基底クラス・PBT・差分ハーネス・テスト用 Processor）"

    "TestFoundation".role {
        summary = "ケース ID 付きテストの基底クラス（AcgSpec など）と @PendingCase（ADR-030）"
        layout { mainKotlin("test-foundation", "testfoundation") / "*".ktFile() }
    }
    "CaseChecker".role {
        summary = "テストケース一覧とテストの宣言を突き合わせる checkCaseIds の実装"
        layout { mainKotlin("test-foundation", "testfoundation") / "check" / "*".ktFile() }
    }
    "PbtFoundation".role {
        summary = "PBT の生成器・縮小・seed・試行数の自動調整（ADR-022）"
        layout {
            mainKotlin("pbt", "pbt") / "*" / "*".ktFile()
            "pbt/src/main/resources/pbt" / "options.tsv".file()
        }
    }
    "DifferentialHarness".role {
        summary = "K / B / N の三者の出力を比べる差分ハーネス（ADR-023）"
        layout { mainKotlin("differential", "differential") / "*".ktFile() }
    }
    "TestProcessor".role {
        summary = "テスト用 Processor TP-*。core（生成）を ksp と native が包む（ADR-027）"
        layout {
            mainKotlin("test-processors/core", "testprocessors") / "*".ktFile()
            mainKotlin("test-processors/ksp", "testprocessors/ksp") / "*".ktFile()
            mainKotlin("test-processors/native", "testprocessors/native") / "*".ktFile()
            "test-processors/ksp/src/main/resources/META-INF/services" /
                "com.google.devtools.ksp.processing.SymbolProcessorProvider".file()
            "test-processors/native/src/main/resources/META-INF/services" /
                "me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorProvider".file()
            "test-processors" / "README.md".file()
        }
    }
    "ModuleTest".role {
        summary = "基盤・テスト補助のモジュール自身の単体テスト（ケース ID を持たない素の FreeSpec を含む）"
        layout {
            testKotlin("analysis-code-gen-testing", "testing") / "**" / "*".ktFile()
            testKotlin("test-foundation", "testfoundation") / "**" / "*".ktFile()
            testKotlin("pbt", "pbt") / "**" / "*".ktFile()
            testKotlin("differential", "differential") / "*".ktFile()
            testKotlin("test-processors/core", "testprocessors") / "*".ktFile()
        }
    }
    "PbtTestResource".role {
        summary = "PBT の単体テストが読むデータ（カタログの縮小版・回帰の置き場の説明）"
        documented = false
        layout {
            "pbt/src/test/resources" {
                "catalog-fixture" {
                    "index.json".file()
                    "patterns" / "*" / "*.kt".file()
                }
                "pbt/regressions" / "README.txt".file()
            }
        }
    }
}

private fun DeclarationContainerScope.caseTestsGroup() = "CaseTests".group {
    summary = "テストケース一覧の ID に結び付いたテスト。観点（ID の接頭辞）ごとのパッケージに置く（ADR-034、ADR-038）"

    val aspects = CASE_ASPECTS.joinToString(",", "{", "}")
    "E2ECaseTest".role {
        summary = "AA に触れない E / P / U のケーステスト（e2e-tests）"
        layout {
            val e2eDir = testKotlin("e2e-tests", "e2e")
            e2eDir {
                "*".ktFile()
                aspects / "*".ktFile()
            }
        }
    }
    "E2ESupport".role {
        summary = "e2e-tests のケーステストが共通で使う補助（フィクスチャの操作・観察・ビルド結果の読み取り）"
        layout { testKotlin("e2e-tests", "e2e") / "support" / "*".ktFile() }
    }
    "EngineCaseTest".role {
        summary = "AA の型に触れる I / U のケーステスト（engine-tests。CI でだけコンパイルできる）"
        layout { testKotlin("engine-tests", "enginetests") / aspects / "*".ktFile() }
    }
    "EngineTestSupport".role {
        summary = "engine-tests のケーステストが共通で使う補助"
        layout { testKotlin("engine-tests", "enginetests") / "support" / "*".ktFile() }
    }
}

private fun DeclarationContainerScope.kspCompatibilityGroup() = "KspCompatibility".group {
    summary = "KSP 公式テスト（KSPT-*）の取り込み（ADR-024）"

    "KsptProcessor".role {
        summary = "KSP の公式テストから移植したテスト用 Processor（パッケージは上流のまま）"
        documented = false
        layout { "ksp-compatibility-tests/src/kspt/kotlin/com/google/devtools/ksp/processor" / "*".ktFile() }
    }
    "KsptTest".role {
        summary = "KSPT-* を KSP2 と Bridge で走らせるテストとランナー"
        layout { testKotlin("ksp-compatibility-tests", "kspcompatibilitytests/kspt") / "*".ktFile() }
    }
    "KsptTestData".role {
        summary = "KSPT-* の入力（上流の testData）と、取り込んだケース・Bridge で有効にしたケースの一覧"
        documented = false
        layout {
            "ksp-compatibility-tests/src/test/resources/kspt" {
                "cases.csv".file()
                "bridge-enabled.txt".file()
                "testData" / "**" / "*.kt".file()
            }
        }
    }
}

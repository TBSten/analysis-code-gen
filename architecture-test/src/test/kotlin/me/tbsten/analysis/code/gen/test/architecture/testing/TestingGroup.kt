@file:OptIn(ExperimentalKatachiApi::class)

package me.tbsten.analysis.code.gen.test.architecture.testing

import me.tbsten.analysis.code.gen.test.architecture.CASE_ASPECTS
import me.tbsten.analysis.code.gen.test.architecture.KOTLIN_COMPILER
import me.tbsten.analysis.code.gen.test.architecture.mainKotlin
import me.tbsten.analysis.code.gen.test.architecture.portedFromHeader
import me.tbsten.analysis.code.gen.test.architecture.testKotlin
import me.tbsten.katachi.ExperimentalKatachiApi
import me.tbsten.katachi.dsl.DeclarationContainerScope
import me.tbsten.katachi.dsl.kotlin.ktFile
import me.tbsten.katachi.konsist.declarations.ktFileContent

// テスト基盤とテストコード（ADR-021〜023、ADR-027、ADR-030、ADR-034、ADR-038）。どれも公開しない内部モジュール
fun DeclarationContainerScope.testingGroup() = "Testing".group {
    summary = "進捗を測るためのテスト基盤とテストコード。公開しない"

    foundationGroup()
    caseTestsGroup()
    kspCompatibilityGroup()

    "BenchmarksModule".role {
        summary = "性能測定のモジュール（中身は今後）"
        layout { mainKotlin("benchmarks", "benchmarks") / "*Module".ktFile() }
    }
    "ArchitectureTest".role {
        summary = "katachi によるファイルの置き場所の定義と検査（ADR-041）"
        layout {
            val definitionDir = testKotlin("architecture-test", "test/architecture")
            definitionDir {
                "{ProjectArchitecture,ProjectArchitectureTest,*Support}".ktFile()
                "*" / "*Group".ktFile()
            }
        }
    }
}

private fun DeclarationContainerScope.foundationGroup() = "Foundation".group {
    summary = "テストを書くための基盤（基底クラス・PBT・差分ハーネス・テスト用 Processor）"

    "TestFoundation".role {
        summary = "ケース ID 付きテストの基底クラス（AcgSpec など）と PendingCase（ADR-030）"
        layout { mainKotlin("test-foundation", "testfoundation") / "*".ktFile() }
        "AA・エンジンに依存しない（どのテストモジュールからも使うため）".ktFileContent {
            `package`(matchingDirectory)
            `import`("**")
            not.`import`(KOTLIN_COMPILER)
            allow(anythingElse)
        }
    }
    "CaseChecker".role {
        summary = "テストケース一覧とテストの宣言を突き合わせる checkCaseIds の実装"
        layout { mainKotlin("test-foundation", "testfoundation") / "check" / "Case*".ktFile() }
    }
    "CaseCheckerSupport".role {
        summary = "checkCaseIds のエントリポイントと CSV の読み書き"
        layout { mainKotlin("test-foundation", "testfoundation") / "check" / "{Main,Csv}".ktFile() }
    }
    "PbtFoundation".role {
        summary = "PBT の生成器・縮小・seed・試行数の自動調整（ADR-022）。領域ごとのサブパッケージに置く"
        layout { mainKotlin("pbt", "pbt") / "{catalog,change,compile,kmp,processor,run,source}" / "*".ktFile() }
    }
    "PbtProcessorOptions".role {
        summary = "PBT が組み合わせる Processor のオプションの一覧"
        layout { "pbt/src/main/resources/pbt" / "options.tsv".file() }
    }
    "DifferentialHarness".role {
        summary = "K / B / N の三者の出力を比べる差分ハーネス（ADR-023）"
        layout { mainKotlin("differential", "differential") / "*".ktFile() }
        "エンジンの内部（AA）に触れず、ランナー越しに比べる".ktFileContent {
            `package`(matchingDirectory)
            `import`("**")
            not.`import`(KOTLIN_COMPILER)
            allow(anythingElse)
        }
    }
    testProcessorGroup()
    "ModuleTest".role {
        summary = "基盤・テスト補助のモジュール自身の単体テスト（ケース ID を持たない素の FreeSpec を含む）"
        layout {
            testKotlin("analysis-code-gen-testing", "testing") / "**" / "*Test".ktFile()
            testKotlin("test-foundation", "testfoundation") / "**" / "*Test".ktFile()
            testKotlin("pbt", "pbt") / "**" / "*Test".ktFile()
            testKotlin("differential", "differential") / "*Test".ktFile()
            testKotlin("test-processors/core", "testprocessors") / "*Test".ktFile()
        }
    }
    "ModuleTestInput".role {
        summary = "単体テストが読む入力（テスト用の Processor・入力の組・サンプルの spec）"
        layout {
            testKotlin("analysis-code-gen-testing", "testing") / "*Provider".ktFile()
            testKotlin("differential", "differential") / "*Inputs".ktFile()
            testKotlin("test-foundation", "testfoundation") / "Samples".ktFile()
        }
    }
    "PbtTestCatalog".group {
        summary = "PBT の単体テストが読む、入力パターンカタログの縮小版"
        documented = false

        "PbtTestCatalogIndex".role {
            summary = "縮小版カタログの索引"
            layout { "pbt/src/test/resources/catalog-fixture" / "index.json".file() }
        }
        "PbtTestCatalogPattern".role {
            summary = "縮小版カタログの入力パターン"
            layout {
                "pbt/src/test/resources/catalog-fixture/patterns/*" / "[A-Z][A-Z][A-Z]-O-[0-9][0-9][0-9][0-9]".ktFile()
            }
        }
    }
    "PbtRegressionNote".role {
        summary = "PBT の回帰の置き場の説明（見つかった反例はここに溜まる）"
        documented = false
        layout { "pbt/src/test/resources/pbt/regressions" / "README.txt".file() }
    }
}

private fun DeclarationContainerScope.testProcessorGroup() = "TestProcessor".group {
    summary = "テスト用 Processor TP-*。core が生成内容を決め、ksp と native が同じ生成物を出すように包む（ADR-027）"

    "TestProcessorCore".role {
        summary = "TP-* の生成内容・名前・オプション・観察イベント"
        layout { mainKotlin("test-processors/core", "testprocessors") / "{Tp*,Probe*}".ktFile() }
        "KSP にも AA にも依存しない（ksp と native の両方から使うため）".ktFileContent {
            `package`(matchingDirectory)
            `import`("java.**")
            `import`("kotlin.**")
            `import`("me.tbsten.analysis.code.gen.testprocessors.**")
            allow(anythingElse)
        }
    }
    "TestProcessorKsp".role {
        summary = "TP-* を KSP の SymbolProcessor として包んだもの"
        layout { mainKotlin("test-processors/ksp", "testprocessors/ksp") / "{TpKsp*,KspSupport}".ktFile() }
    }
    "TestProcessorNative".role {
        summary = "TP-* を Native Processor として包んだもの"
        layout { mainKotlin("test-processors/native", "testprocessors/native") / "{TpNative*,NativeSupport}".ktFile() }
    }
    "TestProcessorServiceRegistration".role {
        summary = "ksp 版・native 版の Processor を ServiceLoader に登録するファイル"
        documented = false
        layout {
            "test-processors/ksp/src/main/resources/META-INF/services" / "com.google.devtools.ksp.processing.SymbolProcessorProvider".file()
            "test-processors/native/src/main/resources/META-INF/services" /
                "me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorProvider".file()
        }
    }
    "TestProcessorReadme".role {
        summary = "TP-* の一覧と使い方"
        documented = false
        layout { "test-processors" / "README.md".file() }
    }
}

private fun DeclarationContainerScope.caseTestsGroup() = "CaseTests".group {
    summary = "テストケース一覧の ID に結び付いたテスト。観点（ID の接頭辞）ごとのパッケージに置く（ADR-034、ADR-038）"

    val aspects = CASE_ASPECTS.joinToString(",", "{", "}")
    "E2ECaseTest".role {
        summary = "AA に触れない E / P / U のケーステスト（e2e-tests）"
        layout { testKotlin("e2e-tests", "e2e") / aspects / "*{Test,Tests}".ktFile() }
        "AA の型に触れない（ADR-034）。ケースは AcgSpec の種別の基底クラスに書く".ktFileContent {
            `package`(matchingDirectory)
            fileAnnotation("OptIn")
            `import`("**")
            not.`import`("org.jetbrains.kotlin.analysis.**")
            required(`class`, "*Test*").superclass(CASE_SPECS)
            allow(anythingElse)
        }
    }
    "E2ESmokeTest".role {
        summary = "e2e-tests の足場が動くことを確かめる、ケース ID の無いテスト"
        layout { testKotlin("e2e-tests", "e2e") / "*Test".ktFile() }
    }
    "E2ESupport".role {
        summary = "e2e-tests のケーステストが共通で使う補助（フィクスチャの操作・観察・ビルド結果の読み取り）"
        layout {
            val e2eDir = testKotlin("e2e-tests", "e2e")
            e2eDir {
                "support" / "*".ktFile()
                aspects / "*Support".ktFile()
            }
        }
        "AA の型に触れない（ADR-034）".ktFileContent {
            `package`(matchingDirectory)
            fileAnnotation("OptIn")
            `import`("**")
            not.`import`("org.jetbrains.kotlin.analysis.**")
            allow(anythingElse)
        }
    }
    "EngineCaseTest".role {
        summary = "AA の型に触れる I / U のケーステスト（engine-tests。CI でだけコンパイルできる）"
        layout { testKotlin("engine-tests", "enginetests") / aspects / "*{Test,Tests}".ktFile() }
        "ケースは AcgSpec の種別の基底クラスに書く".ktFileContent {
            `package`(matchingDirectory)
            fileAnnotation("OptIn")
            `import`("**")
            required(`class`, "*Test*").superclass(CASE_SPECS)
            allow(anythingElse)
        }
    }
    "EngineTestSupport".role {
        summary = "engine-tests のケーステストが共通で使う補助"
        layout {
            val engineDir = testKotlin("engine-tests", "enginetests")
            engineDir {
                "support" / "*".ktFile()
                aspects / "*Support".ktFile()
            }
        }
    }
}

private fun DeclarationContainerScope.kspCompatibilityGroup() = "KspCompatibility".group {
    summary = "KSP 公式テスト（KSPT-*）の取り込み（ADR-024）"

    "KsptProcessor".role {
        summary = "KSP の公式テストから移植したテスト用 Processor。ファイル名とパッケージは上流のまま"
        documented = false
        layout { "ksp-compatibility-tests/src/kspt/kotlin/com/google/devtools/ksp/processor" / "*".ktFile() }
        portedFromHeader()
    }
    "KsptTest".role {
        summary = "KSPT-* を KSP2 と Bridge で走らせるテストとランナー"
        layout { testKotlin("ksp-compatibility-tests", "kspcompatibilitytests/kspt") / "{Kspt*,*Runner}".ktFile() }
    }
    "KsptTestData".role {
        summary = "KSPT-* の入力（上流の testData をそのまま写したもの）"
        documented = false
        layout { "ksp-compatibility-tests/src/test/resources/kspt/testData" / "**" / "*".ktFile() }
        portedFromHeader()
    }
    "KsptCaseList".role {
        summary = "取り込んだ KSPT-* の一覧（import_kspt.py が書く）"
        documented = false
        layout { "ksp-compatibility-tests/src/test/resources/kspt" / "cases.csv".file() }
    }
    "KsptBridgeEnabledList".role {
        summary = "Bridge で有効にした KSPT-* の一覧（Bridge の実装が進むと増える）"
        documented = false
        layout { "ksp-compatibility-tests/src/test/resources/kspt" / "bridge-enabled.txt".file() }
    }
}

/** ケース ID 付きテストの基底クラス（ADR-030）。種別ごとに 1 つ */
private const val CASE_SPECS = "{UnitSpec,IntegrationSpec,E2ESpec,DiffSpec,PbtSpec,BenchSpec}"

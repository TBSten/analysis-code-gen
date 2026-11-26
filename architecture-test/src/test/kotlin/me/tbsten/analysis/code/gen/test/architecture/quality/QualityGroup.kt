package me.tbsten.analysis.code.gen.test.architecture.quality

import me.tbsten.katachi.dsl.DeclarationContainerScope
import me.tbsten.katachi.dsl.kotlin.ktFile

// テストケースの管理と入力パターンカタログ（ADR-020、ADR-030）。quality/ は :quality モジュールでもある
fun DeclarationContainerScope.qualityGroup() = "Quality".group {
    summary = "進捗の分母になるテストケース一覧と、PBT の入力パターンカタログ"

    "TestCaseInventory".role {
        summary = "テストケース一覧の正（ID・種別・マイルストーン）"
        layout { "quality" / "test-case-inventory.md".file() }
    }
    "CaseTable".role {
        summary = "一覧とテストから生成する表（gen_csv.py と updateCaseStatus が書く）と、例外の一覧"
        documented = false
        layout {
            "quality" {
                "test-cases.csv".file()
                "case-status.csv".file()
                "case-exceptions.csv".file()
            }
        }
    }
    "CaseBacklog".role {
        summary = "実装中に気づいたケースの候補（CB-n）。一覧には直接足さない"
        layout { "quality" / "case-backlog.md".file() }
    }
    "QualityDocument".role {
        summary = "quality/ の説明と、PBT・差分テストの設計メモ"
        documented = false
        layout {
            "quality" {
                "README.md".file()
                "pbt-differential-design.md".file()
            }
        }
    }
    "QualityScript".role {
        summary = "一覧とカタログを生成・検査する Python スクリプト"
        documented = false
        layout {
            "quality/scripts" / "*.py".file()
            "quality/catalog/scripts" / "*.py".file()
        }
    }

    "InputPatternCatalog".group {
        summary = "PBT の入力パターンカタログ。1 パターン 1 ファイル（ADR-020）"

        "HandwrittenInputPattern".role {
            summary = "自作の入力パターン（ID の 2 つ目が O）"
            layout { "quality/catalog/patterns/*" / "[A-Z][A-Z][A-Z]-O-[0-9][0-9][0-9][0-9]".ktFile() }
        }
        "ImportedInputPattern".role {
            summary = "テストや実プロジェクトから catalog.py が取り込んだ入力パターン（ID の 2 つ目が T か R）"
            documented = false
            layout { "quality/catalog/patterns/*" / "[A-Z][A-Z][A-Z]-{T,R}-[0-9][0-9][0-9][0-9]".ktFile() }
        }
        "CatalogDimensions".role {
            summary = "入力値空間の次元の定義"
            layout { "quality/catalog" / "dimensions.toml".file() }
        }
        "CatalogIndex".role {
            summary = "catalog.py が生成する索引（PBT が読む）"
            documented = false
            layout { "quality/catalog" / "index.json".file() }
        }
        "CatalogIdMap".role {
            summary = "catalog.py が生成する、取り込み元とパターン ID の対応表"
            documented = false
            layout { "quality/catalog" / "id-map.tsv".file() }
        }
        "CatalogReport".role {
            summary = "カタログの説明と、catalog.py が生成する網羅表・第三者の一覧"
            documented = false
            layout {
                "quality/catalog" {
                    "README.md".file()
                    "coverage.md".file()
                    "THIRD_PARTY.md".file()
                }
            }
        }
        "ThirdPartyLicense".role {
            summary = "取り込んだ入力パターンの出所のライセンス（<owner>__<repo>.txt）"
            documented = false
            layout { "quality/catalog/licenses" / "*__*.txt".file() }
        }
    }
}

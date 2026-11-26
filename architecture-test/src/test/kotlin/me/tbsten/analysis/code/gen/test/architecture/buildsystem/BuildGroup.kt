@file:OptIn(ExperimentalKatachiApi::class)

package me.tbsten.analysis.code.gen.test.architecture.buildsystem

import me.tbsten.analysis.code.gen.test.architecture.conventionPluginRequired
import me.tbsten.analysis.code.gen.test.architecture.independentBuildFiles
import me.tbsten.katachi.ExperimentalKatachiApi
import me.tbsten.katachi.dsl.DeclarationContainerScope
import me.tbsten.katachi.dsl.gradle.gradle
import me.tbsten.katachi.dsl.kotlin.ktFile
import me.tbsten.katachi.dsl.kotlin.ktsFile

// ビルドと、上流から写したコードの出所（ADR-011、ADR-018、ADR-025）
fun DeclarationContainerScope.buildGroup() {
    // wrapper・settings・各モジュールの build.gradle.kts・gradle.properties・version catalog
    gradle {
        buildScript { conventionPluginRequired() }
    }

    "Build".group {
        summary = "ルートのビルドを支える convention plugin と、対応 Kotlin の一覧"
        documented = false

        "BuildLogicScript".role {
            summary = "included build（build-logic）自身のビルドスクリプト"
            layout { "build-logic" / "build.gradle".ktsFile() }
        }
        "BuildLogicSettings".role {
            summary = "included build（build-logic）自身の settings。ルートの version catalog を読む"
            layout { "build-logic" / "settings.gradle".ktsFile() }
        }
        "ConventionPlugin".role {
            summary = "各モジュールが plugins { id(\"acg.*\") } で適用する convention plugin（acg.jvm など）"
            layout { "build-logic/src/main/kotlin" / "acg.*.gradle".ktsFile() }
        }
        "SupportedKotlinVersions".role {
            summary = "対応 Kotlin の唯一の一覧（ADR-011）。版を変えるときはここだけを編集する"
            layout { "gradle" / "kotlin-versions.txt".file() }
        }
    }

    "Porting".group {
        summary = "上流（KSP2・kotlin-compile-testing）から写したコードの出所の記録（ADR-018）"

        "UpstreamPin".role {
            summary = "移植元のリポジトリ・コミット・ライセンスの固定"
            layout { "porting/*" / "UPSTREAM.properties".file() }
        }
        "PortMap".role {
            summary = "上流のファイルと移植先の対応表"
            layout { "porting/*" / "port-map.csv".file() }
        }
        "PortingScript".role {
            summary = "上流から取り込む Python スクリプト（vendor.py・import_kspt.py）"
            documented = false
            layout { "porting/*" / "*.py".file() }
        }
        "UpstreamPatch".role {
            summary = "取り込むときに上流のコードへ当てるパッチ（NNNN-<slug>.patch）"
            documented = false
            layout { "porting/*/patches" / "[0-9][0-9][0-9][0-9]-*.patch".file() }
        }
        "UpstreamLicense".role {
            summary = "上流のライセンス本文（ルートの LICENSE と違うライセンスのとき）"
            documented = false
            layout { "porting/*" / "LICENSE-*.txt".file() }
        }
        "PortingReadme".role {
            summary = "移植の進め方と、いま移植している上流の一覧"
            documented = false
            layout { "porting" / "README.md".file() }
        }
    }

    "Poc".group {
        summary = "P0〜P1 の PoC（ADR-009、ADR-010）。結論は ADR にあり、コードは記録として残す"
        documented = false

        "PocSource".role {
            summary = "PoC のコード"
            layout { "poc/p*" / "**/src" / "**" / "*".ktFile() }
        }
        independentBuildFiles("Poc", "poc/p*", withGradleProperties = true, withWrapper = true)
        "PocRecord".role {
            summary = "PoC の説明・結果・実行と比較のスクリプト"
            layout {
                "poc/p*" {
                    "README.md".file()
                    "results" / "*.md".file()
                    "**" / "*.{py,sh}".file()
                }
            }
        }
    }
}

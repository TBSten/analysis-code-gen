package me.tbsten.analysis.code.gen.test.architecture.fixtures

import me.tbsten.analysis.code.gen.test.architecture.independentBuildFiles
import me.tbsten.katachi.dsl.DeclarationContainerScope
import me.tbsten.katachi.dsl.kotlin.ktFile
import me.tbsten.katachi.dsl.kotlin.ktsFile

/** 共通フィクスチャ F-* のビルド。新しいフィクスチャを足すときはここにも足す（ADR-027） */
private const val COMMON_FIXTURES = "fixtures/projects/{android,java,jvm,jvm-multi,kmp,kmp-custom}"

/** docs のコード例の出所になるビルド。docs の区分に合わせる（ADR-031） */
private const val DOC_FIXTURES = "fixtures/docs/{guides,install,migration,quick-start,testing}"

fun DeclarationContainerScope.fixturesGroup() = "Fixtures".group {
    summary = "テストと docs が使う独立したビルドとデータ（ADR-027、ADR-031）"

    "CommonFixture".group {
        summary = "共通フィクスチャ F-*。E2E・差分テストが Gradle TestKit で動かす独立したビルド"

        "CommonFixtureSource".role {
            summary = "フィクスチャの Kotlin のソース（TP のアノテーションの写しを含む）"
            layout { COMMON_FIXTURES / "**/src/*/kotlin" / "**" / "*".ktFile() }
        }
        "CommonFixtureJavaSource".role {
            summary = "Java と混在するプロジェクトを再現するための Java のソース"
            layout { COMMON_FIXTURES / "**/src/*/java" / "**" / "*.java".file() }
        }
        "AndroidManifest".role {
            summary = "Android フィクスチャのマニフェスト"
            documented = false
            layout { COMMON_FIXTURES / "**/src/main" / "AndroidManifest.xml".file() }
        }
        independentBuildFiles("CommonFixture", COMMON_FIXTURES, withGradleProperties = true, withKspScript = true)
    }

    "FixtureBuildSupport".group {
        summary = "フィクスチャの settings が共通で使う settings plugin のビルド"
        documented = false

        "FixtureSettingsPlugin".role {
            summary = "フィクスチャの settings に共通の設定を足す settings plugin"
            layout { "fixtures/projects/build-support/src/main/kotlin" / "acg.*.settings.gradle".ktsFile() }
        }
        independentBuildFiles("FixtureBuildSupport", "fixtures/projects/build-support")
    }

    "DocFixture".group {
        summary = "docs のコード例の出所。check_snippets が docs と一致するかを検査する（ADR-031）"

        "DocFixtureSource".role {
            summary = "docs に載せるコード例のソース"
            layout { DOC_FIXTURES / "*/src/*/kotlin" / "**" / "*".ktFile() }
        }
        "DocFixtureServiceRegistration".role {
            summary = "コード例の Processor を ServiceLoader に登録するファイル"
            documented = false
            layout { DOC_FIXTURES / "*/src/main/resources/META-INF/services" / "*".file() }
        }
        "DocFixtureReadme".role {
            summary = "コード例のビルドの説明（どの docs のページに対応するか）"
            documented = false
            layout { DOC_FIXTURES / "README.md".file() }
        }
        "DocFixtureExpected".role {
            summary = "コード例が生成するはずのコード（ゴールデン）"
            documented = false
            layout { "fixtures/docs/*/expected" / "**" / "*".ktFile() }
        }
        independentBuildFiles("DocFixture", DOC_FIXTURES)
    }

    "FixturesReadme".role {
        summary = "フィクスチャの一覧と使い方"
        layout { "fixtures" / "README.md".file() }
    }
    "TargetProcessorList".role {
        summary = "移植・検証する既存 Processor の一覧（ADR-019）"
        layout { "fixtures/processors" / "processors.toml".file() }
    }
    "LargeFixtureGenerator".role {
        summary = "F-LARGE を生成するスクリプト（生成物は .gitignore で除外）"
        documented = false
        layout { "fixtures/scripts" / "gen_large.py".file() }
    }
    "LargeFixtureInput".role {
        summary = "F-LARGE の生成が読む依存の一覧"
        documented = false
        layout { "fixtures/large" / "deps-*.txt".file() }
    }
}

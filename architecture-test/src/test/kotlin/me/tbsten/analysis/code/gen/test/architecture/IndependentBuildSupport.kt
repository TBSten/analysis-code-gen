package me.tbsten.analysis.code.gen.test.architecture

import me.tbsten.katachi.dsl.DeclarationContainerScope
import me.tbsten.katachi.dsl.kotlin.ktsFile

/**
 * ルートの settings に含まれない独立した Gradle ビルド（フィクスチャ・PoC）の Gradle のファイルを、種類ごとの Role にする。
 * katachi の gradle() はルートのビルドだけを受け持つため、こちらで書く。
 * [roots] はビルドのルートディレクトリの glob（例: `fixtures/projects/{jvm,kmp}`）
 */
fun DeclarationContainerScope.independentBuildFiles(
    prefix: String,
    roots: String,
    withGradleProperties: Boolean = false,
    withKspScript: Boolean = false,
    withBridgeScript: Boolean = false,
    withWrapper: Boolean = false,
) {
    "${prefix}Settings".role {
        summary = "独立したビルドの settings スクリプト"
        documented = false
        layout { roots / "**" / "settings.gradle".ktsFile() }
    }
    "${prefix}BuildScript".role {
        summary = "独立したビルドの各モジュールのビルドスクリプト"
        documented = false
        layout { roots / "**" / "build.gradle".ktsFile() }
    }
    if (withKspScript) {
        "${prefix}KspBuildScript".role {
            summary = "同じビルドを KSP で動かすときのビルドスクリプト（差分テストの K。build.ksp.gradle.kts）"
            documented = false
            layout { roots / "**" / "build.ksp.gradle".ktsFile() }
        }
    }
    if (withBridgeScript) {
        "${prefix}BridgeBuildScript".role {
            summary = "同じビルドを KSP の Processor + Bridge で動かすときのビルドスクリプト（差分テストの B。build.bridge.gradle.kts）"
            documented = false
            layout { roots / "**" / "build.bridge.gradle".ktsFile() }
        }
    }
    if (withGradleProperties) {
        "${prefix}GradleProperties".role {
            summary = "独立したビルドの gradle.properties"
            documented = false
            layout { roots / "**" / "gradle.properties".file() }
        }
    }
    if (withWrapper) {
        // katachi の gradle() と同じく、起動スクリプト・本体・設定の 3 つに分ける
        "${prefix}GradleWrapperLauncher".role {
            summary = "独立したビルドの wrapper を起動するスクリプト（Unix と Windows）"
            documented = false
            layout {
                roots / "**" {
                    "gradlew".file()
                    "gradlew.bat".file()
                }
            }
        }
        "${prefix}GradleWrapperJar".role {
            summary = "独立したビルドの wrapper の本体"
            documented = false
            layout { roots / "**/gradle/wrapper" / "gradle-wrapper.jar".file() }
        }
        "${prefix}GradleWrapperProperties".role {
            summary = "独立したビルドの wrapper が動かす Gradle の版"
            documented = false
            layout { roots / "**/gradle/wrapper" / "gradle-wrapper.properties".file() }
        }
    }
}

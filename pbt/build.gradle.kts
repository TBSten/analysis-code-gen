// PBT 基盤（ACG-40 / ADR-022）。Arb・縮小・seed 固定・試行数の自動調整・24 分割の網羅層。
// テスト専用の内部モジュール。他モジュールは testImplementation(project(":pbt")) で使う
plugins {
    id("acg.jvm")
    `java-library`
}

kotlin {
    explicitApi()
}

dependencies {
    api(libs.kotest.property)
    api(libs.kotest.assertions.core)
    implementation(libs.kotlinx.serialization.json)
    // 生成したプロジェクトが本当にコンパイルできるかを確かめる（PBT-01）
    implementation(libs.kotlin.compiler.embeddable)
}

// テスト基盤（ACG-37、ADR-030）。全テストの基底クラス・保留ケース・ケース ID 整合チェックを持つ。
// 利用者には公開しない内部モジュール（公開のテスト補助は :analysis-code-gen-testing）
plugins {
    id("acg.jvm")
}

dependencies {
    api(libs.kotest.framework.engine)
}

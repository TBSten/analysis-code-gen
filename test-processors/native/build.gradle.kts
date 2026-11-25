// テスト用 Processor（TP-*）の Native API 版（本命）。analysis-code-gen-processor-api と AA は実行時にエンジンが渡すので compileOnly（ADR-027）
plugins {
    id("acg.library")
    id("acg.analysis-api")
    id("acg.fixture-publish")
}

dependencies {
    api(project(":test-processors:core"))
    compileOnly(project(":analysis-code-gen-processor-api"))
}

// テスト用 Processor（TP-*）の共通部。解析結果のモデルと生成するコードの文字列だけを持ち、AA にも KSP にも依存しない（ADR-027）
plugins {
    id("acg.library")
    id("acg.fixture-publish")
}

tasks.test {
    // TpAnnotationsSourceTest がフィクスチャのコピーを読む
    systemProperty("acg.repoRoot", rootProject.layout.projectDirectory.asFile.absolutePath)
    inputs.files(rootProject.fileTree("fixtures/projects") { include("**/TpAnnotations.kt") }).withPropertyName("tpAnnotationCopies")
}

// cream.kt の Native 書き直し版（RW-CRM-N1、ADR-019・037・071）。processor-api と AA は実行時にエンジンが渡すので compileOnly
plugins {
    id("acg.library")
    id("acg.analysis-api")
    id("acg.fixture-publish")
}

dependencies {
    compileOnly(project(":analysis-code-gen-processor-api"))
}

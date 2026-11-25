// テスト用 Processor（TP-*）の KSP 版。三者差分の K（KSP2 + 元の Processor）側と、フィクスチャが KSP2 でビルドできることの確認に使う（ADR-027）
plugins {
    id("acg.library")
    id("acg.fixture-publish")
}

dependencies {
    api(project(":test-processors:core"))
    compileOnly(libs.ksp.api)
    testImplementation(libs.ksp.api)
}
